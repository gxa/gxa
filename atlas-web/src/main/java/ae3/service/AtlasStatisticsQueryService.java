package ae3.service;

import ae3.service.structuredquery.AtlasBitIndexQueryBuilder;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.index.StatisticsStorageFactory;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.gxa.utils.SizeBoundedLinkedHashMap;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Nov 2, 2010
 * Time: 5:27:03 PM
 * This class provides gene expression statistics query service
 */
public class AtlasStatisticsQueryService implements IndexBuilderEventHandler, DisposableBean {

    final private Logger log = LoggerFactory.getLogger(getClass());

    // Handler for the BitIndex builder
    private IndexBuilder indexBuilder;
    // Bitindex Object which is de-serialized from indexFileName in atlasIndexDir
    private StatisticsStorage statisticsStorage;
    private File atlasIndexDir;
    private String indexFileName;

    // Local cache to avoid re-loading bit stats for a given efo term
    private Map<StatisticsType, SizeBoundedLinkedHashMap<String, Multiset<Long>>> statTypeToEfoToScores
            = new HashMap<StatisticsType, SizeBoundedLinkedHashMap<String, Multiset<Long>>>();

    // Maximum size of the Multiset experiment counts per StatisticsType
    private static final int MAX_STAT_CACHE_SIZE_PER_STAT_TYPE = 500;
    // A flag used to indicate if an attribute for which statistics/experiment counts are being found is an efo or not
    public static final boolean EFO_ATTR = true;

    public AtlasStatisticsQueryService(String indexFileName) {
        this.indexFileName = indexFileName;
    }

    public void setAtlasIndex(File atlasIndexDir) {
        this.atlasIndexDir = atlasIndexDir;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void setStatisticsStorage(StatisticsStorage statisticsStorage) {
        this.statisticsStorage = statisticsStorage;
        populateEfoScoresCache();

    }

    public StatisticsStorage getStatisticsStorage() {
        return statisticsStorage;
    }

    /**
     * Index rebuild notification handler - after BItIndex is re-built, de-serialize it into statisticsStorage and re-populate statTypeToEfoToScores cache
     *
     * @param builder builder
     * @param event   event
     */
    public void onIndexBuildFinish(IndexBuilder builder, IndexBuilderEvent event) {
        StatisticsStorageFactory statisticsStorageFactory = new StatisticsStorageFactory(indexFileName);
        statisticsStorageFactory.setAtlasIndex(atlasIndexDir);
        try {
            statisticsStorage = statisticsStorageFactory.createStatisticsStorage();
            statTypeToEfoToScores = new HashMap<StatisticsType, SizeBoundedLinkedHashMap<String, Multiset<Long>>>(); // clear the cache first
            populateEfoScoresCache();
        } catch (IOException ioe) {
            String errMsg = "Failed to create statisticsStorage from " + atlasIndexDir.getAbsolutePath() + File.separator + indexFileName;
            log.error(errMsg, ioe);
            throw new RuntimeException(errMsg, ioe);
        }
    }

    public void onIndexBuildStart(IndexBuilder builder) {
        // Nothing to do here
    }

    /**
     * Destructor called by Spring
     *
     * @throws Exception
     */
    public void destroy() throws Exception {
        if (indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }

    /**
     * @param statisticType
     * @param efoTerm
     * @return List of GeneConditions, each containing one combination of experimentId-ef-efv corresponding to efoTerm (efoTerm can
     *         correspond to multiple experimentId-ef-efv triples)
     */
    private List<AtlasBitIndexQueryBuilder.GeneCondition> getGeneConditionsForEfo(StatisticsType statisticType, String efoTerm) {
        List<AtlasBitIndexQueryBuilder.GeneCondition> geneConditions = new ArrayList<AtlasBitIndexQueryBuilder.GeneCondition>();
        Set<Pair<Integer, Integer>> attrExpIndexes = statisticsStorage.getEfoIndex().getMappingsForEfo(efoTerm);
        if (attrExpIndexes != null) { // TODO we should log error condition here
            for (Pair<Integer, Integer> indexPair : attrExpIndexes) {
                AtlasBitIndexQueryBuilder.GeneCondition geneCondition = new AtlasBitIndexQueryBuilder.GeneCondition(statisticType);
                geneCondition.inAttribute(indexPair.getFirst()).inExperiment(indexPair.getSecond());
                geneConditions.add(geneCondition);
            }
        }
        return geneConditions;
    }

    /**
     * @param attributes     (OR list of) Attributes for which experiment counts should be found
     * @param statisticsType StatisticsType for which experiment counts should be found
     * @param isEfo          if equal to EFO_ATTR, all attributes are efo terms; otherwise all attributes are ef-efvs
     * @return experiment counts corresponding to attributes and statisticsType
     */
    public Multiset<Long> getExperimentCounts(List<Attribute> attributes, StatisticsType statisticsType, boolean isEfo) {
        AtlasBitIndexQueryBuilder.AtlasQuery atlasQuery = getAtlasOrQuery(attributes, statisticsType, isEfo);
        return scoreQuery(atlasQuery);
    }

    /**
     * @param attributes
     * @param statisticsType
     * @param isEfo          if equal to EFO_ATTR, all attributes are efo terms; otherwise all attributes are ef-efvs
     * @return AtlasQuery needed to find experiment counts for OR list of attributes and statisticsType
     */
    private AtlasBitIndexQueryBuilder.AtlasQuery getAtlasOrQuery(List<Attribute> attributes, StatisticsType statisticsType, boolean isEfo) {
        List<AtlasBitIndexQueryBuilder.GeneCondition> geneConditions = new ArrayList<AtlasBitIndexQueryBuilder.GeneCondition>();
        for (Attribute attr : attributes) {
            if (isEfo == EFO_ATTR) {
                String efoTerm = attr.getEfv();
                geneConditions.addAll(getGeneConditionsForEfo(statisticsType, efoTerm));
            } else { // ef-efv
                Integer attributeIdx = statisticsStorage.getAttributeIndex().getIndexForObject(attr);
                AtlasBitIndexQueryBuilder.GeneCondition geneCondition = new AtlasBitIndexQueryBuilder.GeneCondition(statisticsType);
                geneCondition.inAttribute(attributeIdx);
            }
        }
        return AtlasBitIndexQueryBuilder.constructQuery().where(geneConditions);
    }

    /**
     * @param atlasQuery
     * @return Multiset of aggregated experiment counts, where the set of scores genes is intersected across atlasQuery.getGeneConditions(),
     * and union-ed across attributes within each condition in atlasQuery.getGeneConditions().
     */
    private Multiset<Long> scoreQuery(AtlasBitIndexQueryBuilder.AtlasQuery atlasQuery) {

        Set<AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition>> orGeneConditions = atlasQuery.getGeneConditions();

        Multiset<Long> results = null;

        // run over all or conditions, do "OR" inside (cf. scoreOrGeneConditions()) , "AND"'ing over the whole thing
        for (AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> orGeneCondition : orGeneConditions) {
            Multiset<Long> condGenes = scoreOrGeneConditions(orGeneCondition);

            if (null == results)
                results = condGenes;
            else {
                Iterator<Multiset.Entry<Long>> resultGenes = results.entrySet().iterator();

                while (resultGenes.hasNext()) {
                    Multiset.Entry<Long> entry = resultGenes.next();
                    if (!condGenes.contains(entry.getElement())) // AND operation between different top query conditions
                        resultGenes.remove();
                    else
                        // for all gene ids belonging to intersection of all conditions seen so far, we accumulate experiment counts
                        results.setCount(entry.getElement(), entry.getCount() + condGenes.count(entry.getElement()));
                }
            }

            log.info("[COND AND] ... found " + results.entrySet().size() + " genes for " + orGeneCondition);
        }

        return results;
    }

    /**
     * TODO
     * @param orGeneCondition
     * @return
     */
    private Multiset<Long> scoreOrGeneConditions(AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> orGeneCondition) {
        Multiset<Long> genes = HashMultiset.create();

        for (AtlasBitIndexQueryBuilder.GeneCondition geneCondition : orGeneCondition.getConditions()) {
            Multiset<Long> condGenes = null;

            // run over all or attributes, do "OR" inside (cf. getScoresForAttributes()), "AND"'ing over the whole thing
            for (AtlasBitIndexQueryBuilder.OrConditions<Integer> orAttributeCondition : geneCondition.getAttributeConditions()) {

                Set<Integer> orAttributes = orAttributeCondition.getConditions();
                Multiset<Long> attrGenes = getScoresForAttributes(geneCondition.getStatisticType(), orAttributes);

                if (null == condGenes)
                    condGenes = attrGenes;
                else {
                    Iterator<Multiset.Entry<Long>> entries = condGenes.entrySet().iterator();

                    while (entries.hasNext()) {
                        Multiset.Entry<Long> entry = entries.next();
                        if (!attrGenes.contains(entry.getElement())) // AND operation between attributes inside a single condition
                            entries.remove();
                        else
                            // for all gene ids belonging to intersection for all geneCondition's attributes seen so far , we accumulate experiment counts
                            condGenes.setCount(entry.getElement(), entry.getCount() + attrGenes.count(entry.getElement()));
                    }
                }

                log.info("[ATTR AND] ... found " + condGenes.entrySet().size() + " genes for " + orAttributeCondition);
            }

            genes.addAll(condGenes); 
            log.info("[COND OR] ... found " + genes.entrySet().size() + " genes for " + geneCondition);
        }

        return genes;
    }

    /**
     * @param statisticsType
     * @param attributeIndexes
     * @return Multiset<Long> containing experiment counts corresponding to statisticsType and attributeIndexes
     */
    public Multiset<Long> getScoresForAttributes(final StatisticsType statisticsType, final Set<Integer> attributeIndexes) {
        Multiset<Long> scores = HashMultiset.create();
        for (Integer attrIndex : attributeIndexes) {
            Collection<ConciseSet> stats = getStatisticsForAttribute(statisticsType, attrIndex).values();
            for (ConciseSet stat : stats) {
                scores.addAll(statisticsStorage.getGeneIndex().getObjectsForIndexes(stat));

            }
        }
        return scores;
    }

    /**
     * @param statType
     * @param attrIndex
     * @return Map: experiment index -> bit stats corresponding to statType and statType
     */
    private Map<Integer, ConciseSet> getStatisticsForAttribute(final StatisticsType statType, final Integer attrIndex) {
        Map<Integer, ConciseSet> expsToBits = new HashMap<Integer, ConciseSet>();
        Statistics statistics = statisticsStorage.getStatsForType(statType);
        if (statistics != null) {
            Map<Integer, ConciseSet> expIndexToBits = statistics.getStatisticsForAttribute(attrIndex);
            if (expIndexToBits != null) {
                for (Integer expIndex : expIndexToBits.keySet()) {
                    expsToBits.put(expIndex, expIndexToBits.get(expIndex));
                }
            }
        }
        return Collections.unmodifiableMap(expsToBits);
    }

    /**
     * Method to populate efo to experiment counts cache for UP, DOWN and NON_D_E StatisticsType's
     */
    private void populateEfoScoresCache() {

        log.info("Loading statTypeToEfoToScores cache...");
        if (statisticsStorage == null)
            return;

        long timeStart = System.currentTimeMillis();

        Set<String> efos = statisticsStorage.getEfoIndex().getEfos();
        Set<StatisticsType> statTypesToBeCached = new HashSet<StatisticsType>();

        statTypesToBeCached.add(StatisticsType.UP);
        statTypesToBeCached.add(StatisticsType.DOWN);
        statTypesToBeCached.add(StatisticsType.NON_D_E);

        statTypeToEfoToScores.put(StatisticsType.UP, new SizeBoundedLinkedHashMap<String, Multiset<Long>>(MAX_STAT_CACHE_SIZE_PER_STAT_TYPE));
        statTypeToEfoToScores.put(StatisticsType.DOWN, new SizeBoundedLinkedHashMap<String, Multiset<Long>>(MAX_STAT_CACHE_SIZE_PER_STAT_TYPE));
        statTypeToEfoToScores.put(StatisticsType.NON_D_E, new SizeBoundedLinkedHashMap<String, Multiset<Long>>(MAX_STAT_CACHE_SIZE_PER_STAT_TYPE));

        for (StatisticsType statisticsType : statTypesToBeCached) {
            for (String efo : efos) {
                Multiset<Long> statsSoFar = getExperimentCounts(Collections.singletonList(new Attribute(efo)), statisticsType, EFO_ATTR);
                statTypeToEfoToScores.get(statisticsType).put(efo, statsSoFar);
            }
        }
        log.info("Generated statTypeToEfoToScores cache of " + MAX_STAT_CACHE_SIZE_PER_STAT_TYPE +
                " entries per each StatisticType in " + statTypesToBeCached + " in " + (System.currentTimeMillis() - timeStart) + " ms");
    }
}

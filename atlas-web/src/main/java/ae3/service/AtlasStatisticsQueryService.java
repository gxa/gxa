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
    private Map<StatisticsType, SizeBoundedLinkedHashMap<String, Multiset<Integer>>> statTypeToEfoToScores
            = new HashMap<StatisticsType, SizeBoundedLinkedHashMap<String, Multiset<Integer>>>();

    // Maximum size of the Multiset experiment counts per StatisticsType
    private static final int MAX_STAT_CACHE_SIZE_PER_STAT_TYPE = 500;
    // A flag used to indicate if an attribute for which statistics/experiment counts are being found is an efo or not
    public static final boolean EFO_QUERY = true;

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
            statTypeToEfoToScores = new HashMap<StatisticsType, SizeBoundedLinkedHashMap<String, Multiset<Integer>>>(); // clear the cache first
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
    private AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> getGeneConditionsForEfo(StatisticsType statisticType, String efoTerm) {
        AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> efoConditions =
                new AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition>();
        efoConditions.setEfoTerm(efoTerm);

        Set<Pair<Integer, Integer>> attrExpIndexes = statisticsStorage.getEfoIndex().getMappingsForEfo(efoTerm);
        if (attrExpIndexes != null) { // TODO we should log error condition here
            for (Pair<Integer, Integer> indexPair : attrExpIndexes) {
                AtlasBitIndexQueryBuilder.GeneCondition geneCondition =
                        new AtlasBitIndexQueryBuilder.GeneCondition(statisticType).inAttribute(indexPair.getFirst()).inExperiment(indexPair.getSecond());
                efoConditions.addCondition(geneCondition);
            }
        }
        return efoConditions;
    }


    /**
     *
     * @param geneIds
     * @param attributes
     * @param statisticsType
     * @param isEfo
     * @param minExperiments
     * @return return a subset of geneids if its experiment count >= minExperiments for attributes, statisticsType and isEfo
     */
    public Set<Long> getGenesWithQualifyingCounts(Set<Long> geneIds, List<Attribute> attributes, StatisticsType statisticsType, boolean isEfo, Integer minExperiments) {
        Set<Long> qualifyingGenes = new LinkedHashSet<Long>();
        Multiset<Integer> scores = getExperimentCounts(attributes, statisticsType, isEfo);
        for (Long geneId : geneIds) {
            Integer geneIndex = statisticsStorage.getGeneIndex().getIndexForObject(geneId);
            if (scores.count(geneIndex) >= minExperiments) {
                qualifyingGenes.add(geneId);
            }

        }
        return qualifyingGenes;
    }

    /**
     *
     * @param attributes
     * @param statisticsType
     * @param isEfo indicates that attributes are efo terms rather than efvs
     * @param geneId
     * @return Experiment count for statisticsType, attributes and geneId
     */
    public Integer getExperimentCountsForGene(List<Attribute> attributes, StatisticsType statisticsType, boolean isEfo, Long geneId) {
        Integer geneIndex = statisticsStorage.getGeneIndex().getIndexForObject(geneId);
        return getExperimentCounts(attributes, statisticsType, isEfo).count(geneIndex);
    }

    /**
     * @param attributes     (OR list of) Attributes for which experiment counts should be found
     * @param statisticsType StatisticsType for which experiment counts should be found
     * @param isEfo          if equal to EFO_QUERY, all attributes are efo terms; otherwise all attributes are ef-efvs
     * @return experiment counts corresponding to attributes and statisticsType
     */
    private Multiset<Integer> getExperimentCounts(List<Attribute> attributes, StatisticsType statisticsType, boolean isEfo) {
        
        AtlasBitIndexQueryBuilder.GeneCondition atlasQuery = getAtlasQuery(attributes, statisticsType, isEfo);

        return scoreQuery(atlasQuery);
    }

    /**
     * @param attributes
     * @param statisticsType
     * @param isEfo          if equal to EFO_ATTR, all attributes are efo terms; otherwise all attributes are ef-efvs
     * @return AtlasQuery needed to find experiment counts for OR list of attributes and statisticsType
     */
    private AtlasBitIndexQueryBuilder.GeneCondition getAtlasQuery(List<Attribute> attributes, StatisticsType statisticsType, boolean isEfo) {
        AtlasBitIndexQueryBuilder.GeneCondition geneCondition = AtlasBitIndexQueryBuilder.constructQuery().where(statisticsType);
        for (Attribute attr : attributes) {
            if (isEfo == EFO_QUERY) {
                String efoTerm = attr.getEfv();
                AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> efoConditions = getGeneConditionsForEfo(statisticsType, efoTerm);
                geneCondition.and(efoConditions);
            } else { // ef-efv
                Integer attributeIdx = statisticsStorage.getAttributeIndex().getIndexForObject(attr);
                geneCondition.inAttribute(attributeIdx);
            }
        }
        return geneCondition;
    }

    /**
     * @param atlasQuery
     * @return Multiset of aggregated experiment counts, where the set of scores genes is intersected across atlasQuery.getGeneConditions(),
     *         and union-ed across attributes within each condition in atlasQuery.getGeneConditions().
     */
    private Multiset<Integer> scoreQuery(AtlasBitIndexQueryBuilder.GeneCondition atlasQuery) {

        Set<AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition>> andGeneConditions = atlasQuery.getConditions();

        Multiset<Integer> results = null;

        // run over all or conditions, do "OR" inside (cf. scoreOrGeneConditions()) , "AND"'ing over the whole thing
        for (AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> orConditions : andGeneConditions) {

            Multiset<Integer> condGenes = null;

            condGenes = getScoresForOrConditions(orConditions);

            if (null == results)
                results = condGenes;
            else {
                Iterator<Multiset.Entry<Integer>> resultGenes = results.entrySet().iterator();

                while (resultGenes.hasNext()) {
                    Multiset.Entry<Integer> entry = resultGenes.next();
                    if (!condGenes.contains(entry.getElement())) // AND operation between different top query conditions
                        resultGenes.remove();
                    else
                        // for all gene ids belonging to intersection of all conditions seen so far, we accumulate experiment counts
                        results.setCount(entry.getElement(), entry.getCount() + condGenes.count(entry.getElement()));
                }
            }

            log.debug("[COND AND] ... found " + results.entrySet().size() + " genes for " + atlasQuery);
        }
        return results;
    }

    /**
     * @param statisticsType
     * @param attributeIndexes
     * @return Multiset<Long> containing experiment counts corresponding to statisticsType and attributeIndexes
     */
    private Multiset<Integer> getScoresForAttributes(final StatisticsType statisticsType, final Set<Integer> attributeIndexes) {
        Multiset<Integer> scores = HashMultiset.create();
        for (Integer attrIndex : attributeIndexes) {
            Collection<ConciseSet> stats = getStatisticsForAttribute(statisticsType, attrIndex).values();
            for (ConciseSet stat : stats) {
                scores.addAll(stat);

            }
        }
        return scores;
    }

    /**
     * @param orConditions AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition>
     * @return Multiset<Integer> containing experiment counts corresponding to all attributes indexes in each GeneCondition in orConditions
     */
    private Multiset<Integer> getScoresForOrConditions(AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> orConditions) {
        String efoTerm = orConditions.getEfoTerm();
        Iterator<AtlasBitIndexQueryBuilder.GeneCondition> iter = orConditions.getConditions().iterator();
        StatisticsType statisticsTypeForEfo = null;
        if (iter.hasNext()) {
            // Assumption: if orConditions represent experiment id-ef-efv mappings for a single efo, they will all share the same StatisticsType
            statisticsTypeForEfo = iter.next().getStatisticsType();
        } else {
            log.error("Found no mapping to experiments-ef-efvs for efo: " + efoTerm);
            // TODO Should RuntimeException be thrown here??
            return HashMultiset.create();
        }

        if (statTypeToEfoToScores.get(statisticsTypeForEfo).containsKey(efoTerm)) { // If scores already in cache - return
            return statTypeToEfoToScores.get(statisticsTypeForEfo).get(efoTerm);
        }

        Multiset<Integer> scores = HashMultiset.create();
        for (AtlasBitIndexQueryBuilder.GeneCondition orCondition : orConditions.getConditions()) {
            for (Integer attrIdx : orCondition.getAttributes()) {
                Map<Integer, ConciseSet> expsToStats = getStatisticsForAttribute(orCondition.getStatisticsType(), attrIdx);
                for (Integer expIdx : orCondition.getExperiments()) {
                    if (expsToStats.get(expIdx) != null) {
                        scores.addAll(expsToStats.get(expIdx));
                    } else {
                        Experiment exp = (Experiment) statisticsStorage.getExperimentIndex().getObjectForIndex(expIdx);
                        Attribute attr = (Attribute) statisticsStorage.getAttributeIndex().getObjectForIndex(attrIdx);
                        log.debug("Failed to retrieve stats for: " + orCondition.getStatisticsType() + " : " + exp + " : " + attr);
                    }
                }
            }
        }
        if (efoTerm != null) { // add scores to cache
            statTypeToEfoToScores.get(statisticsTypeForEfo).put(efoTerm, scores);
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

        statTypeToEfoToScores.put(StatisticsType.UP, new SizeBoundedLinkedHashMap<String, Multiset<Integer>>(MAX_STAT_CACHE_SIZE_PER_STAT_TYPE));
        statTypeToEfoToScores.put(StatisticsType.DOWN, new SizeBoundedLinkedHashMap<String, Multiset<Integer>>(MAX_STAT_CACHE_SIZE_PER_STAT_TYPE));
        statTypeToEfoToScores.put(StatisticsType.NON_D_E, new SizeBoundedLinkedHashMap<String, Multiset<Integer>>(MAX_STAT_CACHE_SIZE_PER_STAT_TYPE));

        for (StatisticsType statisticsType : statTypesToBeCached) {
            for (String efo : efos) {
                Multiset<Integer> expCounts = getExperimentCounts(Collections.singletonList(new Attribute(efo)), statisticsType, EFO_QUERY);
                statTypeToEfoToScores.get(statisticsType).put(efo, expCounts);
                if (statTypeToEfoToScores.get(statisticsType).size() >= MAX_STAT_CACHE_SIZE_PER_STAT_TYPE) {
                    break;
                }
            }
            log.info("Generated statTypeToEfoToScores cache of " + MAX_STAT_CACHE_SIZE_PER_STAT_TYPE + " entries " + statisticsType);
        }
        log.info("Generated statTypeToEfoToScores cache of " + MAX_STAT_CACHE_SIZE_PER_STAT_TYPE +
                " entries per each StatisticType in " + statTypesToBeCached + " in " + (System.currentTimeMillis() - timeStart) + " ms");
    }
}

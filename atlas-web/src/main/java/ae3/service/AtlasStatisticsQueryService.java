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

    private static final String PRETTY_PRINT_OFFSET = "  ";

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
    private static final int MAX_STAT_CACHE_SIZE_PER_STAT_TYPE = 1000;
    // Experiment scores with number of genes in experiment scores will not be cached as are fast enough to retrieve at runtime
    private static final int MIN_NUM_GENES_IN_CACHED_EXP_SCORES = 1000;
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
                efoConditions.orCondition(geneCondition);
            }
        }
        return efoConditions;
    }


    /**
     * @param efvOrEfo
     * @param statisticsType
     * @param isEfo
     * @param geneId
     * @return Experiment count for statisticsType, attributes and geneId
     */
    public Integer getExperimentCountsForGene(String efvOrEfo, StatisticsType statisticsType, boolean isEfo, Long geneId) {
        Integer geneIndex = statisticsStorage.getGeneIndex().getIndexForObject(geneId);
        Multiset<Integer> scores = getExperimentCounts(new Attribute(efvOrEfo, isEfo), statisticsType);
        if (scores != null) {
            int expCountForGene = scores.count(geneIndex);
            if (expCountForGene > 0) {
                log.debug(statisticsType + " " + efvOrEfo +  " expCountForGene: " + geneId + " (" + geneIndex + ") = " + expCountForGene);
            }
            return expCountForGene;
        }
        return 0;
    }

    /**
     * @param attribute      Attribute for which experiment counts should be found
     * @param statisticsType StatisticsType for which experiment counts should be found
     * @return experiment counts corresponding to attributes and statisticsType
     */
    private Multiset<Integer> getExperimentCounts(Attribute attribute, StatisticsType statisticsType) {

        AtlasBitIndexQueryBuilder.GeneCondition atlasQuery = getAtlasQuery(Collections.singletonList(Collections.singletonList(attribute)), statisticsType);

        long start = System.currentTimeMillis();
        Multiset<Integer> counts = scoreQuery(atlasQuery);
        if (counts.size() > 0) {
            log.debug("AtlasQuery: " + prettyPrintAtlasQuery(atlasQuery, "") + " ==> result set size: " + counts.size() + " (duration: " + (System.currentTimeMillis() - start) + " ms)");
        }

        return counts;
    }

    private String prettyPrintAtlasQuery(AtlasBitIndexQueryBuilder.GeneCondition atlasQuery, String offset) {
        StringBuilder sb = new StringBuilder();
        Set<AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition>> andGeneConditions = atlasQuery.getConditions();
        if (!andGeneConditions.isEmpty()) {
            sb.append("\n").append(offset).append(" [ ");
            int i = 0;
            for (AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> orConditions : andGeneConditions) {
                if (orConditions.getEfoTerm() != null) {
                    sb.append(" efo: " + orConditions.getEfoTerm() + " -> ");
                }
                for (AtlasBitIndexQueryBuilder.GeneCondition geneCondition : orConditions.getConditions()) {
                    sb.append(prettyPrintAtlasQuery(geneCondition, offset + PRETTY_PRINT_OFFSET));
                }
                if (++i < andGeneConditions.size())
                    sb.append(" AND ");
            }
            sb.append("\n").append(offset).append(" ] ");
        } else { // TODO end of recursion

            Set<Integer> attrs = atlasQuery.getAttributes();
            Set<Integer> exps = atlasQuery.getExperiments();

            // Output attributes
            if (!attrs.isEmpty()) {
                sb.append("in attrs: [ ");
                int i = 0;
                for (Integer attrIdx : attrs) {
                    Attribute attr = (Attribute) statisticsStorage.getAttributeIndex().getObjectForIndex(attrIdx);
                    sb.append(attr);
                    if (++i < attrs.size())
                        sb.append(" OR ");
                }
                sb.append(" ] ");
                if (!exps.isEmpty()) {
                    sb.append(" AND ");
                }
            }
            // Output experiments

            if (!exps.isEmpty()) {

                sb.append("in exps: [ ");
                int i = 0;
                for (Integer expIdx : exps) {
                    Experiment exp = (Experiment) statisticsStorage.getExperimentIndex().getObjectForIndex(expIdx);
                    sb.append(exp.getAccession());
                    if (++i < attrs.size())
                        sb.append(" OR ");
                }
                sb.append(" ] ");
            }
        }
        return sb.toString();
    }

    /**
     * @param andListOfOrConditions
     * @param statisticsType
     * @return AtlasQuery needed to find experiment counts for OR list of attributes and statisticsType
     */
    private AtlasBitIndexQueryBuilder.GeneCondition getAtlasQuery(List<List<Attribute>> andListOfOrConditions, StatisticsType statisticsType) {
        AtlasBitIndexQueryBuilder.GeneCondition atlasQuery = AtlasBitIndexQueryBuilder.constructQuery().where(statisticsType);
        for (List<Attribute> orAttrs : andListOfOrConditions) {
            atlasQuery.and(getAtlasOrQuery(orAttrs, statisticsType));
        }
        return atlasQuery;
    }

    private AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> getAtlasOrQuery(List<Attribute> orAttributes, StatisticsType statisticsType) {
        AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> orConditions =
                new AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition>();

        for (Attribute attr : orAttributes) {
            AtlasBitIndexQueryBuilder.GeneCondition cond = AtlasBitIndexQueryBuilder.constructQuery().where(statisticsType);
            if (attr.isEfo() == EFO_QUERY) {
                String efoTerm = attr.getEfv();
                AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> efoConditions = getGeneConditionsForEfo(statisticsType, efoTerm);
                cond.and(efoConditions);
            } else { // ef-efv
                Integer attributeIdx = statisticsStorage.getAttributeIndex().getIndexForObject(attr);
                cond.inAttribute(attributeIdx);
            }
            orConditions.orCondition(cond);
        }
        return orConditions;
    }

    /**
     * @param atlasQuery
     * @return Multiset of aggregated experiment counts, where the set of scores genes is intersected across atlasQuery.getGeneConditions(),
     *         and union-ed across attributes within each condition in atlasQuery.getGeneConditions().
     */
    private Multiset<Integer> scoreQuery(AtlasBitIndexQueryBuilder.GeneCondition atlasQuery) {

        Set<AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition>> andGeneConditions = atlasQuery.getConditions();

        Multiset<Integer> results = null;

        if (andGeneConditions.isEmpty()) { // TODO end of recursion
            results = HashMultiset.create();
            for (Integer attrIdx : atlasQuery.getAttributes()) {
                Map<Integer, ConciseSet> expsToStats = getStatisticsForAttribute(atlasQuery.getStatisticsType(), attrIdx);
                if (expsToStats.isEmpty()) {
                    Attribute attr = (Attribute) statisticsStorage.getAttributeIndex().getObjectForIndex(attrIdx);
                    log.info("Failed to retrieve stats for stat: " + atlasQuery.getStatisticsType() + " and attr: " + attr);
                } else {
                    Set<Integer> expIdxs = atlasQuery.getExperiments();
                    if (expIdxs.isEmpty()) {
                        expIdxs = expsToStats.keySet();
                    }
                    for (Integer expIdx : atlasQuery.getExperiments()) {
                        if (expsToStats.get(expIdx) != null) {
                            results.addAll(expsToStats.get(expIdx));
                        } else {
                            Experiment exp = (Experiment) statisticsStorage.getExperimentIndex().getObjectForIndex(expIdx);
                            Attribute attr = (Attribute) statisticsStorage.getAttributeIndex().getObjectForIndex(attrIdx);
                            log.debug("Failed to retrieve stats for stat: " + atlasQuery.getStatisticsType() + " exp: " + exp.getAccession() + " and attr: " + attr);
                        }
                    }
                }
            }
        } else {
            // run over all or conditions, do "OR" inside (cf. scoreOrGeneConditions()) , "AND"'ing over the whole thing
            for (AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> orConditions : andGeneConditions) {
                // process OR conditions
                Multiset<Integer> condGenes = getScoresForOrConditions(orConditions, atlasQuery.getStatisticsType());

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
            }
        }

        if (results == null) {
            results = HashMultiset.create();
        }
        return results;
    }

    /**
     * @param orConditions AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition>
     * @return Multiset<Integer> containing experiment counts corresponding to all attributes indexes in each GeneCondition in orConditions
     */
    private Multiset<Integer> getScoresForOrConditions(
            AtlasBitIndexQueryBuilder.OrConditions<AtlasBitIndexQueryBuilder.GeneCondition> orConditions, StatisticsType statType) {

        String efoTerm = orConditions.getEfoTerm();

        if (efoTerm != null && statTypeToEfoToScores.get(statType).containsKey(efoTerm)) { // If scores already in cache - return
            return statTypeToEfoToScores.get(statType).get(efoTerm);
        }

        Multiset<Integer> scores = HashMultiset.create();
        for (AtlasBitIndexQueryBuilder.GeneCondition orCondition : orConditions.getConditions()) {
            scores.addAll(scoreQuery(orCondition));
        }
        if (efoTerm != null) {
            // add scores to cache
            statTypeToEfoToScores.get(statType).put(efoTerm, scores);
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
        statTypesToBeCached.add(StatisticsType.UP_DOWN);
        statTypesToBeCached.add(StatisticsType.NON_D_E);

        for (StatisticsType statisticsType : statTypesToBeCached) {
            statTypeToEfoToScores.put(statisticsType, new SizeBoundedLinkedHashMap<String, Multiset<Integer>>(MAX_STAT_CACHE_SIZE_PER_STAT_TYPE));
            for (String efo : efos) {
                if (statTypeToEfoToScores.get(statisticsType).size() >= MAX_STAT_CACHE_SIZE_PER_STAT_TYPE) {
                    break;
                }
                Multiset<Integer> expCounts = getExperimentCounts(new Attribute(efo, EFO_QUERY), statisticsType);
                if (expCounts.size() > MIN_NUM_GENES_IN_CACHED_EXP_SCORES) {
                    statTypeToEfoToScores.get(statisticsType).put(efo, expCounts);
                }
            }
            log.info("Generated statTypeToEfoToScores cache of " + statTypeToEfoToScores.get(statisticsType).size() + " entries for statistic: " + statisticsType);
        }
    }
}

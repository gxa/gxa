package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.Pair;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: petryszaks
 * Date: 20-Dec-2010
 * Time: 16:12:45
 * To change this template use File | Settings | File Templates.
 */
public class StatisticsQueryUtils {

    static final private Logger log = LoggerFactory.getLogger(StatisticsQueryUtils.class);

    // A flag used to indicate if an attribute for which statistics/experiment counts are being found is an efo or not
    public static final boolean EFO_QUERY = true;

    /**
     * TODO
     * @param orAttributes
     * @param statisticsStorage
     * @return
     */
    public static StatisticsQueryOrConditions<StatisticsQueryCondition> getAtlasOrQuery(
            List<Attribute> orAttributes,
            StatisticsStorage statisticsStorage) {
        StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions =
                new StatisticsQueryOrConditions<StatisticsQueryCondition>();

        for (Attribute attr : orAttributes) {
            StatisticsQueryCondition cond = new StatisticsQueryCondition(attr.getStatType());

            if (attr.isEfo() == EFO_QUERY) {
                String efoTerm = attr.getEfv();
                StatisticsQueryOrConditions<StatisticsQueryCondition> efoConditions = getConditionsForEfo(attr.getStatType(), efoTerm, statisticsStorage);
                cond.and(efoConditions);

            } else { // ef-efv
                Integer attributeIdx = statisticsStorage.getIndexForAttribute(attr);
                if (attributeIdx != null) {
                    cond.inAttribute(attr);
                } else {
                    log.debug("Attribute " + attr + " was not found in Attribute Index");
                }
            }
            orConditions.orCondition(cond);
        }
        return orConditions;
    }

    /**
     * @param statisticType
     * @param efoTerm
     * @return List of GeneConditions, each containing one combination of experimentId-ef-efv corresponding to efoTerm (efoTerm can
     *         correspond to multiple experimentId-ef-efv triples)
     */
    private static StatisticsQueryOrConditions<StatisticsQueryCondition> getConditionsForEfo(
            StatisticsType statisticType,
            String efoTerm,
            StatisticsStorage statisticsStorage
    ) {
        StatisticsQueryOrConditions<StatisticsQueryCondition> efoConditions =
                new StatisticsQueryOrConditions<StatisticsQueryCondition>();
        efoConditions.setEfoTerm(efoTerm);

        Set<Pair<Integer, Integer>> attrExpIndexes = statisticsStorage.getMappingsForEfo(efoTerm);
        if (attrExpIndexes != null) { // TODO we should log error condition here
            for (Pair<Integer, Integer> indexPair : attrExpIndexes) {
                Attribute attr = statisticsStorage.getAttributeForIndex(indexPair.getFirst());
                Experiment exp = statisticsStorage.getExperimentForIndex(indexPair.getSecond());
                StatisticsQueryCondition geneCondition =
                        new StatisticsQueryCondition(statisticType).inAttribute(attr).inExperiment(exp);
                efoConditions.orCondition(geneCondition);
            }
        }
        return efoConditions;
    }

    /**
     * @param atlasQuery
     * @return Multiset of aggregated experiment counts, where the set of scores genes is intersected across atlasQuery.getGeneConditions(),
     *         and union-ed across attributes within each condition in atlasQuery.getGeneConditions().
     */
    public static Multiset<Integer> scoreQuery(
            StatisticsQueryCondition atlasQuery,
            StatisticsStorage statisticsStorage) {

        Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andGeneConditions = atlasQuery.getConditions();

        Multiset<Integer> results = null;

        if (andGeneConditions.isEmpty()) { // TODO end of recursion
            ConciseSet geneRestrictionIdxs = null;
            if (atlasQuery.getGeneRestrictionSet() != null) {
                geneRestrictionIdxs = statisticsStorage.getIndexesForGeneIds(atlasQuery.getGeneRestrictionSet());
            }

            Set<Attribute> attributes = atlasQuery.getAttributes();
            if (attributes.isEmpty()) {
                // No attributes were provided - we have to use pre-computed scores across all attributes
                Multiset<Integer> scoresAcrossAllEfos = statisticsStorage.getScoresAcrossAllEfos(atlasQuery.getStatisticsType());
                results = intersect(scoresAcrossAllEfos, geneRestrictionIdxs);
            } else {

                results = HashMultiset.create();
                //************* TODO Score once per experiment - across all attributes
                for (Experiment exp : atlasQuery.getExperiments()) {
                    Integer expIdx = statisticsStorage.getIndexForExperiment(exp);
                    ConciseSet statsForExperiment = new ConciseSet();
                    for (Attribute attr : attributes) {
                        Integer attrIdx = statisticsStorage.getIndexForAttribute(attr);
                        Map<Integer, ConciseSet> expsToStats = getStatisticsForAttribute(atlasQuery.getStatisticsType(), attrIdx, statisticsStorage);
                        if (expsToStats.isEmpty()) {
                            log.debug("Failed to retrieve stats for stat: " + atlasQuery.getStatisticsType() + " and attr: " + attr);
                        } else {
                            if (expsToStats.get(expIdx) != null) {
                                statsForExperiment.addAll(intersect(expsToStats.get(expIdx), geneRestrictionIdxs));
                            } else {
                                log.debug("Failed to retrieve stats for stat: " + atlasQuery.getStatisticsType() + " exp: " + exp.getAccession() + " and attr: " + attr);
                            }
                        }
                    }
                     results.addAll(statsForExperiment);
                }

                //*************

                for (Attribute attr : attributes) {
                    Integer attrIdx = statisticsStorage.getIndexForAttribute(attr);
                    Map<Integer, ConciseSet> expsToStats = getStatisticsForAttribute(atlasQuery.getStatisticsType(), attrIdx, statisticsStorage);
                    if (expsToStats.isEmpty()) {
                        log.debug("Failed to retrieve stats for stat: " + atlasQuery.getStatisticsType() + " and attr: " + attr);
                    } else {
                        Set<Experiment> exps = atlasQuery.getExperiments();
                        if (exps.isEmpty()) { // No experiments conditions were specified - collect scores for all experiments for attr
                            atlasQuery.inExperiments(statisticsStorage.getExperimentsForIndexes(expsToStats.keySet()));
                        }
                        for (Experiment exp : atlasQuery.getExperiments()) {
                            Integer expIdx = statisticsStorage.getIndexForExperiment(exp);
                            if (expsToStats.get(expIdx) != null) {
                                results.addAll(intersect(expsToStats.get(expIdx), geneRestrictionIdxs));
                            } else {
                                log.debug("Failed to retrieve stats for stat: " + atlasQuery.getStatisticsType() + " exp: " + exp.getAccession() + " and attr: " + attr);
                            }
                        }
                    }
                }
            }
        } else {
            // run over all or conditions, do "OR" inside (cf. scoreOrGeneConditions()) , "AND"'ing over the whole thing
            for (StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions : andGeneConditions) {

                // Pass gene restriction set down to orConditions
                orConditions.setGeneRestrictionSet(atlasQuery.getGeneRestrictionSet());
                // process OR conditions
                Multiset<Integer> condGenes = getScoresForOrConditions(orConditions, statisticsStorage);

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
     * @param statType
     * @param attrIndex
     * @return Map: experiment index -> bit stats corresponding to statType and statType
     */
    private static Map<Integer, ConciseSet> getStatisticsForAttribute(
            final StatisticsType statType,
            final Integer attrIndex,
            final StatisticsStorage statisticsStorage) {
        Map<Integer, ConciseSet> expIndexToBits = statisticsStorage.getStatisticsForAttribute(attrIndex, statType);
        if (expIndexToBits != null) {
            return expIndexToBits;
        }
        return Collections.unmodifiableMap(new HashMap<Integer, ConciseSet>());
    }

    /**
     * @param set
     * @param restrictionSet
     * @return intersection of set and restrictionSet (if restrictionSet non-null & non-empty); otherwise return set
     */
    private static ConciseSet intersect(final ConciseSet set, final ConciseSet restrictionSet) {
        if (restrictionSet != null && !restrictionSet.isEmpty()) {
            int prevSize = set.size();
            ConciseSet intersection = new ConciseSet(set);
            intersection.retainAll(restrictionSet);
            log.debug(prevSize != 0 ? ("Size saving by retainAll = " + (((prevSize - intersection.size()) * 100) / prevSize)) + "%" : "");
            return intersection;
        }
        return set;
    }

    /**
     * @param scores
     * @param restrictionSet
     * @return intersection of set and restrictionSet (if restrictionSet non-null & non-empty); otherwise return set
     */
    private static Multiset<Integer> intersect(final Multiset<Integer> scores, final ConciseSet restrictionSet) {
        if (restrictionSet != null && !restrictionSet.isEmpty()) {
            int prevSize = scores.size();
            Multiset<Integer> intersection = HashMultiset.create(scores);
            intersection.retainAll(restrictionSet);
            log.debug(prevSize != 0 ? ("Size saving by retainAll = " + (((prevSize - intersection.size()) * 100) / prevSize)) + "%" : "");
            return intersection;
        }
        return scores;
    }

    /**
     * @param orConditions StatisticsQueryOrConditions<StatisticsQueryCondition>
     * @return Multiset<Integer> containing experiment counts corresponding to all attributes indexes in each GeneCondition in orConditions
     */
    private static Multiset<Integer> getScoresForOrConditions(
            StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions,
            StatisticsStorage statisticsStorage) {

        Multiset<Integer> scores = HashMultiset.create();
        for (StatisticsQueryCondition orCondition : orConditions.getConditions()) {
            orCondition.setGeneRestrictionSet(orConditions.getGeneRestrictionSet());
            scores.addAll(scoreQuery(orCondition, statisticsStorage));
        }
        return scores;
    }

    /**
     * @param statType
     * @return Multiset<Integer> containing experiment counts across all efo attributes
     */
    public static Multiset<Integer> getScoresAcrossAllEfos(
            final StatisticsType statType,
            final StatisticsStorage statisticsStorage) {
        List<Attribute> efoAttrs = new ArrayList<Attribute>();
        Set<String> efos = statisticsStorage.getEfos();
        for (String efo : efos) {
            efoAttrs.add(new Attribute(efo, EFO_QUERY, statType));
        }
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(statType);
        statsQuery.and(getAtlasOrQuery(efoAttrs, statisticsStorage));
        Multiset<Integer> counts = getExperimentCounts(statsQuery, statisticsStorage);
        return counts;
    }

    /**
     * @param statsQuery StatisticsQueryCondition
     * @return experiment counts corresponding to attributes and statisticsType
     */
    public static Multiset<Integer> getExperimentCounts(
            StatisticsQueryCondition statsQuery,
            StatisticsStorage statisticsStorage) {
        long start = System.currentTimeMillis();
        Multiset<Integer> counts = StatisticsQueryUtils.scoreQuery(statsQuery, statisticsStorage);
        long dur = System.currentTimeMillis() - start;
        int numOfGenesWithCunts = counts.elementSet().size();
        if (numOfGenesWithCunts > 0) {
            log.debug("AtlasQuery: " + statsQuery.prettyPrint("") + " ==> result set size: " + numOfGenesWithCunts + " (duration: " + dur + " ms)");
        }

        return counts;
    }
}

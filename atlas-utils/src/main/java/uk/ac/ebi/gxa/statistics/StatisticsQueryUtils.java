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
 * This class handles Statistics queries, delegated from AtlasStatisticsQueryService
 * (as well as from GeneAtlasBitIndexBuilderService when caching experiment counts across all efo's)
 */
public class StatisticsQueryUtils {

    static final private Logger log = LoggerFactory.getLogger(StatisticsQueryUtils.class);

    // A flag used to indicate if an attribute for which statistics/experiment counts are being found is an efo or not
    public static final boolean EFO_QUERY = true;

    /**
     * @param orAttributes
     * @param statisticsStorage - used to retrieve indexes of orAttributes, needed finding experiment counts in bit index
     * @return StatisticsQueryOrConditions representing orAttributes
     */
    public static StatisticsQueryOrConditions<StatisticsQueryCondition> getStatisticsOrQuery(
            List<Attribute> orAttributes,
            StatisticsStorage statisticsStorage) {

        StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions =
                new StatisticsQueryOrConditions<StatisticsQueryCondition>();

        Map<Integer, Set<Integer>> allExpsToAttrs = new HashMap<Integer, Set<Integer>>();

        StatisticsType statType = null;

        for (Attribute attr : orAttributes) {
            StatisticsQueryCondition cond = new StatisticsQueryCondition(attr.getStatType());
            if (statType == null) {
                // All clauses of OR queries share the same statisticsType, hence we only
                // need to retrieve it once.
                statType = attr.getStatType();
            }

            if (attr.isEfo() == EFO_QUERY) {
                String efoTerm = attr.getEfv();
                getConditionsForEfo(efoTerm, statisticsStorage, allExpsToAttrs);

            } else { // ef-efv
                Integer attributeIdx = statisticsStorage.getIndexForAttribute(attr);
                if (attributeIdx != null) {
                    cond.inAttribute(attr);
                } else {
                    log.debug("Attribute " + attr + " was not found in Attribute Index");
                }
                orConditions.orCondition(cond);
            }
        }

        // Now process allExpsToAttrs - for all efo terms in orAttributes, grouping into one StatisticsQueryCondition
        // attributes from potentially different efoTerms for one experiment. This has the effect of counting a given
        // experiment only once OR collection of Attributes.
        for (Integer expIdx : allExpsToAttrs.keySet()) {
            Experiment exp = statisticsStorage.getExperimentForIndex(expIdx);
            StatisticsQueryCondition cond =
                    new StatisticsQueryCondition(statType).inExperiments(Collections.singletonList(exp));
            for (Integer attrIdx : allExpsToAttrs.get(expIdx)) {
                Attribute attr = statisticsStorage.getAttributeForIndex(attrIdx);
                cond.inAttribute(attr);
            }
            orConditions.orCondition(cond);
        }
            
        return orConditions;
    }

    /**
     * @param efoTerm
     * @param statisticsStorage - used to obtain indexes of attributes and experiments, needed finding experiment counts in bit index
     * @param allExpsToAttrs Map: experiment index -> Set<Attribute Index> to which mappings for efoterm are to be added
     * @return OR list of StatisticsQueryConditions, each containing one combination of experimentId-ef-efv corresponding to efoTerm (efoTerm can
     *         correspond to multiple experimentId-ef-efv triples). Note that we group conditions for a given efo term per experiment.
     *         This is so that when the query is scored, we don't count the experiment multiple times for a given efo term.
     */
    private static void getConditionsForEfo(
            final String efoTerm,
            final StatisticsStorage statisticsStorage,
            Map<Integer, Set<Integer>> allExpsToAttrs
    ) {

        Map<Integer, Set<Integer>> expsToAttr = statisticsStorage.getMappingsForEfo(efoTerm);
        if (expsToAttr != null) {
            for (Integer expIdx : expsToAttr.keySet()) {
                if (!allExpsToAttrs.containsKey(expIdx)) {
                    allExpsToAttrs.put(expIdx, new HashSet<Integer>());
                }
                allExpsToAttrs.get(expIdx).addAll(expsToAttr.get(expIdx));
            }
        } else {
            String errMsg = "No mapping to experiments-efvs was found for efo term: " + efoTerm;
            log.debug(errMsg);
            // TODO Is this necessary? throw new RuntimeException(errMsg);
        }
    }

    /**
     * If no experiments were specified, inject into statisticsQuery a superset of all experiments for which stats exist across all attributes
     *
     * @param statisticsQuery
     * @param statisticsStorage
     */
    private static void setQueryExperiments(StatisticsQueryCondition statisticsQuery, StatisticsStorage statisticsStorage) {
        Set<Experiment> exps = statisticsQuery.getExperiments();
        if (exps.isEmpty()) { // No experiments conditions were specified - assemble a superset of all experiments for which stats exist across all attributes
            for (Attribute attr : statisticsQuery.getAttributes()) {
                Integer attrIdx = statisticsStorage.getIndexForAttribute(attr);
                Map<Integer, ConciseSet> expsToStats = getStatisticsForAttribute(statisticsQuery.getStatisticsType(), attrIdx, statisticsStorage);
                exps.addAll(statisticsStorage.getExperimentsForIndexes(expsToStats.keySet()));
            }
            statisticsQuery.inExperiments(exps);
        }
    }

    /**
     * The core scoring method for statistics queries
     * @param statisticsQuery
     * @return Multiset of aggregated experiment counts, where the set of scores genes is intersected across statisticsQuery.getConditions(),
     *         and union-ed across attributes within each condition in statisticsQuery.getConditions().
     */
    public static Multiset<Integer> scoreQuery(
            StatisticsQueryCondition statisticsQuery,
            StatisticsStorage statisticsStorage) {

        Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andStatisticsQueryConditions = statisticsQuery.getConditions();

        Multiset<Integer> results = null;

        if (andStatisticsQueryConditions.isEmpty()) { // End of recursion
            ConciseSet geneRestrictionIdxs = null;
            if (statisticsQuery.getGeneRestrictionSet() != null) {
                geneRestrictionIdxs = statisticsStorage.getIndexesForGeneIds(statisticsQuery.getGeneRestrictionSet());
            }

            Set<Attribute> attributes = statisticsQuery.getAttributes();
            if (attributes.isEmpty()) {

                // No attributes were provided - we have to use pre-computed scores across all attributes
                Multiset<Integer> scoresAcrossAllEfos = statisticsStorage.getScoresAcrossAllEfos(statisticsQuery.getStatisticsType());
                results = intersect(scoresAcrossAllEfos, geneRestrictionIdxs);
            } else {

                results = HashMultiset.create();
                setQueryExperiments(statisticsQuery, statisticsStorage);

                // For each experiment in the query, traverse through all attributes and add all gene indexes into one ConciseSet. This way a gene can score
                // only once for a single experiment - across all OR attributes in this query. Once all attributes have been traversed for a single experiment,
                // add ConciseSet to Multiset results
                for (Experiment exp : statisticsQuery.getExperiments()) {
                    Integer expIdx = statisticsStorage.getIndexForExperiment(exp);
                    ConciseSet statsForExperiment = new ConciseSet();
                    for (Attribute attr : attributes) {
                        Integer attrIdx = statisticsStorage.getIndexForAttribute(attr);
                        Map<Integer, ConciseSet> expsToStats = getStatisticsForAttribute(statisticsQuery.getStatisticsType(), attrIdx, statisticsStorage);
                        if (expsToStats.isEmpty()) {
                            log.debug("Failed to retrieve stats for stat: " + statisticsQuery.getStatisticsType() + " and attr: " + attr);
                        } else {
                            if (expsToStats.get(expIdx) != null) {
                                statsForExperiment.addAll(intersect(expsToStats.get(expIdx), geneRestrictionIdxs));
                            } else {
                                log.debug("Failed to retrieve stats for stat: " + statisticsQuery.getStatisticsType() + " exp: " + exp.getAccession() + " and attr: " + attr);
                            }
                        }
                    }
                    results.addAll(statsForExperiment);
                }
            }
        } else {
            // run over all AND conditions, do "OR" inside (cf. scoreOrStatisticsQueryConditions()) , "AND"'ing over the whole thing
            for (StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions : andStatisticsQueryConditions) {

                // Pass gene restriction set down to orConditions
                orConditions.setGeneRestrictionSet(statisticsQuery.getGeneRestrictionSet());
                // process OR conditions
                Multiset<Integer> condGenes = getScoresForOrConditions(orConditions, statisticsStorage);

                if (results == null)
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
     * @param statisticsStorage
     * @return Map: experiment index -> bit stats corresponding to statType and attrIndex
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
     * @return intersection of set (ConciseSet) and restrictionSet (if restrictionSet non-null & non-empty); otherwise return set
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
     * @return intersection of set (Multiset<Integer>) and restrictionSet (if restrictionSet non-null & non-empty); otherwise return set
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
     * @param statisticsStorage
     * @return Multiset<Integer> containing experiment counts corresponding to all attribute indexes in each StatisticsQueryCondition in orConditions
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
     * @param statisticsStorage
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
        statsQuery.and(getStatisticsOrQuery(efoAttrs, statisticsStorage));
        Multiset<Integer> counts = getExperimentCounts(statsQuery, statisticsStorage);
        return counts;
    }

    /**
     * @param statsQuery StatisticsQueryCondition
     * @return experiment counts corresponding for statsQuery
     */
    public static Multiset<Integer> getExperimentCounts(
            StatisticsQueryCondition statsQuery,
            StatisticsStorage statisticsStorage) {
        long start = System.currentTimeMillis();
        Multiset<Integer> counts = StatisticsQueryUtils.scoreQuery(statsQuery, statisticsStorage);
        long dur = System.currentTimeMillis() - start;
        int numOfGenesWithCunts = counts.elementSet().size();
        if (numOfGenesWithCunts > 0) {
            log.debug("StatisticsQuery: " + statsQuery.prettyPrint() + " ==> result set size: " + numOfGenesWithCunts + " (duration: " + dur + " ms)");
        }
        return counts;
    }
}

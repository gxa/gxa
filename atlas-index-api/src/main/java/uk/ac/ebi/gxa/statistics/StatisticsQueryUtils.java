package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;
import it.uniroma3.mat.extendedset.ExtendedSet;
import it.uniroma3.mat.extendedset.FastSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.Collections.emptyMap;

/**
 * This class handles Statistics queries, delegated from AtlasStatisticsQueryService
 * <p/>
 * (as well as from GeneAtlasBitIndexBuilderService when caching experiment counts across all efo's)
 */
public class StatisticsQueryUtils {

    static final private Logger log = LoggerFactory.getLogger(StatisticsQueryUtils.class);

    /**
     * @param orAttributes
     * @param minExperiments    minimum experiment count restriction for this clause
     * @param statisticsStorage - used to retrieve orAttributes, needed finding experiment counts in bit index
     * @return StatisticsQueryOrConditions representing orAttributes
     */
    public static StatisticsQueryOrConditions<StatisticsQueryCondition> getStatisticsOrQuery(
            List<Attribute> orAttributes,
            int minExperiments,
            StatisticsStorage statisticsStorage) {

        StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions =
                new StatisticsQueryOrConditions<StatisticsQueryCondition>();

        orConditions.setMinExperiments(minExperiments);

        // LinkedHashMap used to maintain ordering of processing of experiments in multi-Attribute, multi-Experiment bit index queries to
        // retrieve sorted lists of experiments to be plotted on the gene page.
        Map<ExperimentInfo, Set<EfvAttribute>> allExpsToAttrs = new LinkedHashMap<ExperimentInfo, Set<EfvAttribute>>();

        StatisticsType statType = null;

        for (Attribute attr : orAttributes) {
            if (statType == null)
                // All clauses of OR queries share the same statisticsType, hence we only
                // need to retrieve it once.
                statType = attr.getStatType();

            attr.getEfvExperimentMappings(statisticsStorage, allExpsToAttrs);
        }

        // Now process allExpsToAttrs - for all efo terms in orAttributes, grouping into one StatisticsQueryCondition
        // attributes from potentially different efoTerms for one experiment. This has the effect of counting a given
        // experiment only once for an OR collection of Attributes.
        for (Map.Entry<ExperimentInfo, Set<EfvAttribute>> expToAttr : allExpsToAttrs.entrySet()) {
            StatisticsQueryCondition cond = new StatisticsQueryCondition(statType);
            if (expToAttr.getKey() != EfvAttribute.ALL_EXPERIMENTS_PLACEHOLDER)
                // For efv Attributes we span all experiments
                cond.inExperiments(Collections.singletonList(expToAttr.getKey()));
            for (EfvAttribute attr : expToAttr.getValue()) {
                cond.inAttribute(attr.withStatType(statType));
            }
            orConditions.orCondition(cond);
        }

        return orConditions;
    }

    /**
     * The core scoring method for statistics queries
     *
     * @param statisticsQuery   query to be peformed on statisticsStorage
     * @param statisticsStorage core data for Statistics qeries
     * @param scoringExps       an out parameter.
     *                          <p/>
     *                          - If null, experiment counts result of statisticsQuery should be returned. if
     *                          - If non-null, it serves as a flag that an optimised statisticsQuery should be performed to just collect
     *                          Experiments for which non-zero counts exist for Statistics query. A typical call scenario in this case is
     *                          just one efv per statisticsQuery, in which we can both:
     *                          1. check if the efv Attribute itself is a scoring one
     *                          2. map this Attribute and Experimeants in scoringExps to efo terms - via the reverse mapping efv-experiment-> efo term
     *                          in EfoIndex (c.f. atlasStatisticsQueryService.getScoringAttributesForGenes())
     * @return Multiset of aggregated experiment counts, where the set of scores genes is intersected across statisticsQuery.getConditions(),
     *         and union-ed across attributes within each condition in statisticsQuery.getConditions().
     */
    public static Multiset<Integer> scoreQuery(
            StatisticsQueryCondition statisticsQuery,
            final StatisticsStorage statisticsStorage,
            Set<ExperimentInfo> scoringExps) {

        // gatherScoringExpsOnly -> experiment counts should be calculated for statisticsQuery
        // !gatherScoringExpsOnly -> scoring experiments should be collected (into scoringExps) only
        boolean gatherScoringExpsOnly = scoringExps != null;
        Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andStatisticsQueryConditions = statisticsQuery.getConditions();

        Multiset<Integer> results = null;

        if (andStatisticsQueryConditions.isEmpty()) { // End of recursion
            Set<Integer> bioEntityIdRestrictionSet = statisticsQuery.getBioEntityIdRestrictionSet();

            Set<EfvAttribute> attributes = statisticsQuery.getAttributes();
            if (attributes.isEmpty()) {

                // No attributes were provided - we have to use pre-computed scores across all attributes
                Multiset<Integer> scoresAcrossAllEfos = statisticsStorage.getScoresAcrossAllEfos(statisticsQuery.getStatisticsType());
                results = intersect(scoresAcrossAllEfos, bioEntityIdRestrictionSet);
            } else {
                results = HashMultiset.create();
                setQueryExperiments(statisticsQuery, statisticsStorage);

                // For each experiment in the query, traverse through all attributes and add all gene indexes into one ConciseSet. This way a gene can score
                // only once for a single experiment - across all OR attributes in this query. Once all attributes have been traversed for a single experiment,
                // add ConciseSet to Multiset results
                for (ExperimentInfo exp : statisticsQuery.getExperiments()) {
                    FastSet statsForExperiment = new FastSet();
                    for (EfvAttribute attr : attributes) {
                        Map<ExperimentInfo, ConciseSet> expsToStats = getStatisticsForAttribute(attr.getStatType(), attr, statisticsStorage);
                        if (expsToStats != null) {
                            if (expsToStats.isEmpty()) {
                                log.debug("Failed to retrieve stats for stat: " + attr.getStatType() + " and attr: " + attr);
                            } else {
                                if (expsToStats.get(exp) != null) {
                                    if (!gatherScoringExpsOnly) {
                                        statsForExperiment.addAll(intersect(expsToStats.get(exp), bioEntityIdRestrictionSet));
                                    } else if (containsAtLeastOne(expsToStats.get(exp), bioEntityIdRestrictionSet)) {
                                        // exp contains at least one non-zero score for at least one gene index in bioEntityIdRestrictionSet -> add it to scoringExps
                                        scoringExps.add(exp);
                                    }
                                } else {
                                    log.debug("Failed to retrieve stats for stat: " + attr.getStatType() + " exp: " + exp.getAccession() + " and attr: " + attr);
                                }
                            }
                        }
                    }
                    if (!gatherScoringExpsOnly) {
                        results.addAll(statsForExperiment);
                    }
                }
            }
        } else {
            // run over all AND conditions, do "OR" inside (cf. scoreOrStatisticsQueryConditions()) , "AND"'ing over the whole thing
            for (StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions : andStatisticsQueryConditions) {

                // Pass gene restriction set down to orConditions
                orConditions.setGeneRestrictionSet(statisticsQuery.getBioEntityIdRestrictionSet());
                // process OR conditions
                Multiset<Integer> condGenes = getScoresForOrConditions(orConditions, statisticsStorage, scoringExps);

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
     * @param statisticsStorage
     * @return Multiset<Integer> containing experiment counts across all efo attributes
     */
    public static Multiset<Integer> getScoresAcrossAllEfos(
            final StatisticsType statType,
            final StatisticsStorage statisticsStorage) {
        List<Attribute> efoAttrs = new ArrayList<Attribute>();
        for (String efo : statisticsStorage.getEfos()) {
            efoAttrs.add(new EfoAttribute(efo, statType));
        }
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(statType);
        statsQuery.and(getStatisticsOrQuery(efoAttrs, 1, statisticsStorage));
        return getExperimentCounts(statsQuery, statisticsStorage, null);
    }

    /**
     * @param statsQuery        StatisticsQueryCondition
     * @param statisticsStorage
     * @param scoringExps       Set of experiments that have at least one non-zero score for statisticsQuery. This is used retrieving efos
     *                          to be displayed in heatmap when no query efvs exist (c.f. atlasStatisticsQueryService.getScoringAttributesForGenes())
     * @return experiment counts corresponding for statsQuery
     */
    public static Multiset<Integer> getExperimentCounts(
            StatisticsQueryCondition statsQuery,
            StatisticsStorage statisticsStorage,
            Set<ExperimentInfo> scoringExps) {
        long start = System.currentTimeMillis();
        Multiset<Integer> counts = StatisticsQueryUtils.scoreQuery(statsQuery, statisticsStorage, scoringExps);
        long dur = System.currentTimeMillis() - start;
        int numOfGenesWithCounts = counts.elementSet().size();
        if (numOfGenesWithCounts > 0) {
            log.debug("StatisticsQuery: " + statsQuery.prettyPrint() + " ==> result set size: " + numOfGenesWithCounts + " (duration: " + dur + " ms)");
        }
        return counts;
    }

    /**
     * Populate bestExperimentsSoFar with an (unsorted) list of experiments with best pval/tstat rank, for statisticsQuery
     *
     * @param statisticsQuery
     * @param statisticsStorage
     * @param bestExperimentsSoFar
     */
    public static void getBestExperiments(
            StatisticsQueryCondition statisticsQuery,
            final StatisticsStorage statisticsStorage,
            Map<Long, ExperimentResult> bestExperimentsSoFar) {
        Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andStatisticsQueryConditions = statisticsQuery.getConditions();


        if (andStatisticsQueryConditions.isEmpty()) { // End of recursion
            Set<Integer> bioEntityIdRestrictionSet = statisticsQuery.getBioEntityIdRestrictionSet();

            Set<EfvAttribute> attributes = statisticsQuery.getAttributes();
            Set<ExperimentInfo> experiments = statisticsQuery.getExperiments();

            for (EfvAttribute attr : attributes) {

                SortedMap<PTRank, Map<ExperimentInfo, ConciseSet>> pValToExpToGenes =
                        statisticsStorage.getPvalsTStatRanksForAttribute(attr, statisticsQuery.getStatisticsType());

                if (pValToExpToGenes != null) {
                    for (Map.Entry<PTRank, Map<ExperimentInfo, ConciseSet>> pValToExpToGenesEntry : pValToExpToGenes.entrySet()) {
                        Map<ExperimentInfo, ConciseSet> expToGenes = pValToExpToGenesEntry.getValue();
                        if (expToGenes != null) {
                            for (Map.Entry<ExperimentInfo, ConciseSet> expToGenesEntry : expToGenes.entrySet()) {
                                if (experiments.isEmpty() || experiments.contains(expToGenesEntry.getKey())) {
                                    if (containsAtLeastOne(expToGenesEntry.getValue(), bioEntityIdRestrictionSet)) {
                                        // If best experiments are collected for an (OR) group of genes, pVal/tStat
                                        // for any of these genes will be considered here
                                        ExperimentResult expCandidate = new ExperimentResult(expToGenesEntry.getKey());
                                        // TODO: 4alf: mutability strikes here!
                                        expCandidate.setPValTstatRank(pValToExpToGenesEntry.getKey());
                                        expCandidate.setHighestRankAttribute(attr);
                                        tryAddOrReplaceExperiment(expCandidate, bestExperimentsSoFar);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // We only expect one 'AND' condition with set of orConditions inside
            StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions = andStatisticsQueryConditions.iterator().next();
            if (orConditions != null) {
                for (StatisticsQueryCondition orCondition : orConditions.getConditions()) {
                    // Pass gene restriction set down to orCondition
                    orCondition.setBioEntityIdRestrictionSet(orConditions.getBioEntityIdRestrictionSet());
                    getBestExperiments(orCondition, statisticsStorage, bestExperimentsSoFar);
                }
            }
        }
    }

    /**
     * If no experiments were specified, inject into statisticsQuery a superset of all experiments for which stats exist across all attributes
     *
     * @param statisticsQuery
     * @param statisticsStorage
     */
    private static void setQueryExperiments(StatisticsQueryCondition statisticsQuery, StatisticsStorage statisticsStorage) {
        Set<ExperimentInfo> exps = statisticsQuery.getExperiments();
        if (exps.isEmpty()) { // No experiments conditions were specified - assemble a superset of all experiments for which stats exist across all attributes
            for (EfvAttribute attr : statisticsQuery.getAttributes()) {
                Map<ExperimentInfo, ConciseSet> expsToStats = getStatisticsForAttribute(attr.getStatType(), attr, statisticsStorage);
                if (expsToStats != null)
                    exps.addAll(expsToStats.keySet());
            }
            statisticsQuery.inExperiments(exps);
        }
    }


    /**
     * Used by scoreQuery() to find out if a given attribute-experiment combination yields a non-zero count for at least
     * one gene index in geneRestrictionIdxs
     *
     * @param counts
     * @param geneRestrictionIdxs
     * @return true of counts contains at least one element of geneRestrictionIdxs.
     */
    private static boolean containsAtLeastOne(ConciseSet counts, Set<Integer> geneRestrictionIdxs) {
        for (Integer geneIdx : geneRestrictionIdxs) {
            if (counts.contains(geneIdx)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param statType
     * @param attribute
     * @param statisticsStorage
     * @return Map: experiment -> bit stats corresponding to statType and attr
     */
    private static Map<ExperimentInfo, ConciseSet> getStatisticsForAttribute(
            final StatisticsType statType,
            final EfvAttribute attribute,
            final StatisticsStorage statisticsStorage) {
        Map<ExperimentInfo, ConciseSet> expToBits = statisticsStorage.getStatisticsForAttribute(attribute, statType);
        if (expToBits != null) {
            return expToBits;
        }
        return emptyMap();
    }

    /**
     * @param set
     * @param restrictionSet
     * @return intersection of set (ConciseSet) and restrictionSet (if restrictionSet non-null & non-empty); otherwise return set
     */
    private static ExtendedSet<Integer> intersect(final ExtendedSet<Integer> set, final Set<Integer> restrictionSet) {
        if (restrictionSet != null && !restrictionSet.isEmpty()) {
            int prevSize = set.size();
            FastSet intersection = new FastSet(set);
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
    public static Multiset<Integer> intersect(final Multiset<Integer> scores, final Set<Integer> restrictionSet) {
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
     * @param orConditions      StatisticsQueryOrConditions<StatisticsQueryCondition>
     * @param statisticsStorage
     * @param scoringExps       Set of experiments that have at least one non-zero score for statisticsQuery. This is used retrieving efos
     *                          to be displayed in heatmap when no query efvs exist (c.f. atlasStatisticsQueryService.getScoringAttributesForGenes())
     * @return Multiset<Integer> containing experiment counts corresponding to all attributes in each StatisticsQueryCondition in orConditions
     */
    private static Multiset<Integer> getScoresForOrConditions(
            final StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions,
            StatisticsStorage statisticsStorage,
            Set<ExperimentInfo> scoringExps) {

        Multiset<Integer> scores = HashMultiset.create();
        for (StatisticsQueryCondition orCondition : orConditions.getConditions()) {
            orCondition.setBioEntityIdRestrictionSet(orConditions.getBioEntityIdRestrictionSet());
            scores.addAll(scoreQuery(orCondition, statisticsStorage, scoringExps));
        }

        // Now apply orConditions' min experiments restriction to scores
        Multiset<Integer> qualifyingScores = HashMultiset.create();
        for (Multiset.Entry<Integer> entry : scores.entrySet()) {
            if (entry.getCount() >= orConditions.getMinExperiments()) {
                qualifyingScores.setCount(entry.getElement(), entry.getCount());
            }
        }

        return qualifyingScores;
    }


    /**
     * If exp cannot be found in exps, add it to exps
     * If it can be found and its pVal/tStat ranks are worse the one is exps, replace it in exps
     *
     * @param exp
     * @param exps
     */
    private static void tryAddOrReplaceExperiment(ExperimentResult exp, Map<Long, ExperimentResult> exps) {
        long expId = exp.getExperimentId();
        ExperimentResult existingExp = exps.get(expId);
        if (existingExp != null) {
            if (exp.getPValTStatRank().compareTo(existingExp.getPValTStatRank()) < 0) {
                exps.put(expId, exp);
            }
        } else {
            exps.put(expId, exp);
        }
    }
}

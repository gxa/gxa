package uk.ac.ebi.gxa.statistics;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
     * @param statisticsStorage - used to retrieve indexes of orAttributes, needed finding experiment counts in bit index
     * @return StatisticsQueryOrConditions representing orAttributes
     */
    public static StatisticsQueryOrConditions<StatisticsQueryCondition> getStatisticsOrQuery(
            List<Attribute> orAttributes,
            int minExperiments,
            StatisticsStorage<Long> statisticsStorage) {

        StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions =
                new StatisticsQueryOrConditions<StatisticsQueryCondition>();

        orConditions.setMinExperiments(minExperiments);

        // LinkedHashMap used to maintain ordering of processing of experiments in multi-Attribute, multi-Experiment bit index queries to
        // retrieve sorted lists of experiments to be plotted on the gene page.
        Map<Experiment, Set<EfvAttribute>> allExpsToAttrs = new LinkedHashMap<Experiment, Set<EfvAttribute>>();

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
        for (Map.Entry<Experiment, Set<EfvAttribute>> expToAttr : allExpsToAttrs.entrySet()) {
            StatisticsQueryCondition cond = new StatisticsQueryCondition(statType);
            if (expToAttr.getKey() != EfvAttribute.ALL_EXPERIMENTS_PLACEHOLDER)
                // For efv Attributes we span all experiments
                cond.inExperiments(Collections.singletonList(expToAttr.getKey()));
            for (EfvAttribute attr : expToAttr.getValue()) {
                attr.setStatType(statType);
                cond.inAttribute(attr);
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
            final StatisticsStorage<Long> statisticsStorage,
            Set<Experiment> scoringExps) {

        // gatherScoringExpsOnly -> experiment counts should be calculated for statisticsQuery
        // !gatherScoringExpsOnly -> scoring experiments should be collected (into scoringExps) only
        boolean gatherScoringExpsOnly = scoringExps != null;
        Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andStatisticsQueryConditions = statisticsQuery.getConditions();

        Multiset<Integer> results = null;

        if (andStatisticsQueryConditions.isEmpty()) { // End of recursion
            ConciseSet geneRestrictionIdxs = null;
            if (statisticsQuery.getGeneRestrictionSet() != null) {
                geneRestrictionIdxs = statisticsStorage.getIndexesForGeneIds(statisticsQuery.getGeneRestrictionSet());
            }

            Set<EfvAttribute> attributes = statisticsQuery.getAttributes();
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
                        Integer attrIdx = statisticsStorage.getIndexForAttribute((EfvAttribute) attr);
                        if (attrIdx != null) {
                            Map<Integer, ConciseSet> expsToStats = getStatisticsForAttribute(attr.getStatType(), attrIdx, statisticsStorage);
                            if (expsToStats.isEmpty()) {
                                log.debug("Failed to retrieve stats for stat: " + attr.getStatType() + " and attr: " + attr);
                            } else {
                                if (expsToStats.get(expIdx) != null) {
                                    if (!gatherScoringExpsOnly) {
                                        statsForExperiment.addAll(intersect(expsToStats.get(expIdx), geneRestrictionIdxs));
                                    } else if (containsAtLeastOne(expsToStats.get(expIdx), geneRestrictionIdxs)) {
                                        // exp contains at least one non-zero score for at least one gene index in geneRestrictionIdxs -> add it to scoringExps
                                        scoringExps.add(exp);
                                    }
                                } else {
                                    log.debug("Failed to retrieve stats for stat: " + attr.getStatType() + " exp: " + exp.getAccession() + " and attr: " + attr);
                                }
                            }
                        } else {
                            // TODO NB. This is currently possible as sample properties are not currently stored in statisticsStorage
                            log.debug("Attribute " + attr + " was not found in Attribute Index");
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
                orConditions.setGeneRestrictionSet(statisticsQuery.getGeneRestrictionSet());
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
            final StatisticsStorage<Long> statisticsStorage) {
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
            StatisticsStorage<Long> statisticsStorage,
            Set<Experiment> scoringExps) {
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
            final StatisticsStorage<Long> statisticsStorage,
            List<Experiment> bestExperimentsSoFar) {
        Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andStatisticsQueryConditions = statisticsQuery.getConditions();


        if (andStatisticsQueryConditions.isEmpty()) { // End of recursion
            ConciseSet geneRestrictionIdxs = null;
            if (statisticsQuery.getGeneRestrictionSet() != null) {
                geneRestrictionIdxs = statisticsStorage.getIndexesForGeneIds(statisticsQuery.getGeneRestrictionSet());
            }

            Set<EfvAttribute> attributes = statisticsQuery.getAttributes();
            for (EfvAttribute attr : attributes) {
                Integer attrIdx = statisticsStorage.getIndexForAttribute(attr);

                SortedMap<PvalTstatRank, Map<Integer, ConciseSet>> pValToExpToGenes =
                        statisticsStorage.getPvalsTStatRanksForAttribute(attrIdx, statisticsQuery.getStatisticsType());

                if (pValToExpToGenes != null) {
                    for (Map.Entry<PvalTstatRank, Map<Integer, ConciseSet>> pValToExpToGenesEntry : pValToExpToGenes.entrySet()) {
                        Map<Integer, ConciseSet> expToGenes = pValToExpToGenesEntry.getValue();
                        if (expToGenes != null) {
                            for (Map.Entry<Integer, ConciseSet> expToGenesEntry : expToGenes.entrySet()) {
                                Integer expIdx = expToGenesEntry.getKey();
                                if (containsAtLeastOne(expToGenesEntry.getValue(), geneRestrictionIdxs)) {
                                    // If best experiments are collected for an (OR) group of genes, pVal/tStat
                                    // for any of these genes will be considered here
                                    Experiment exp = statisticsStorage.getExperimentForIndex(expIdx);
                                    Experiment expCandidate = new Experiment(exp.getAccession(), exp.getExperimentId());
                                    expCandidate.setPvalTstatRank(pValToExpToGenesEntry.getKey());
                                    expCandidate.setHighestRankAttribute(attr);
                                    tryAddOrReplaceExperiment(expCandidate, bestExperimentsSoFar);
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
                    orCondition.setGeneRestrictionSet(orConditions.getGeneRestrictionSet());
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
    private static void setQueryExperiments(StatisticsQueryCondition statisticsQuery, StatisticsStorage<Long> statisticsStorage) {
        Set<Experiment> exps = statisticsQuery.getExperiments();
        if (exps.isEmpty()) { // No experiments conditions were specified - assemble a superset of all experiments for which stats exist across all attributes
            for (EfvAttribute attr : statisticsQuery.getAttributes()) {
                Integer attrIdx = statisticsStorage.getIndexForAttribute(attr);
                if (attrIdx != null) {
                    Map<Integer, ConciseSet> expsToStats = getStatisticsForAttribute(attr.getStatType(), attrIdx, statisticsStorage);
                    exps.addAll(statisticsStorage.getExperimentsForIndexes(expsToStats.keySet()));
                } else {
                    // TODO NB. This is currently possible as sample properties are not currently stored in statisticsStorage
                    log.debug("Attribute " + attr + " was not found in Attribute Index");
                }
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
     * @param attrIndex
     * @param statisticsStorage
     * @return Map: experiment index -> bit stats corresponding to statType and attrIndex
     */
    private static Map<Integer, ConciseSet> getStatisticsForAttribute(
            final StatisticsType statType,
            final Integer attrIndex,
            final StatisticsStorage<Long> statisticsStorage) {
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
    public static Multiset<Integer> intersect(final Multiset<Integer> scores, final ConciseSet restrictionSet) {
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
     * @return Multiset<Integer> containing experiment counts corresponding to all attribute indexes in each StatisticsQueryCondition in orConditions
     */
    private static Multiset<Integer> getScoresForOrConditions(
            final StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions,
            StatisticsStorage<Long> statisticsStorage,
            Set<Experiment> scoringExps) {

        Multiset<Integer> scores = HashMultiset.create();
        for (StatisticsQueryCondition orCondition : orConditions.getConditions()) {
            orCondition.setGeneRestrictionSet(orConditions.getGeneRestrictionSet());
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
    private static void tryAddOrReplaceExperiment(Experiment exp, List<Experiment> exps) {
        Integer idx = exps.indexOf(exp);
        if (idx != -1) {
            if (exp.getpValTStatRank().compareTo(exps.get(idx).getpValTStatRank()) < 0) {
                exps.set(idx, exp);
            }
        } else {
            exps.add(exp);
        }
    }

    /**
     * @param t
     * @return tStat ranks as follows:
     *         t =<  -9       -> rank: -4
     *         t in <-6, -9)  -> rank: -3
     *         t in <-3, -6)  -> rank: -2
     *         t in (-3,  0)  -> rank: -1
     *         t == 0         -> rank:  0
     *         t in ( 0,  3)  -> rank:  1
     *         t in < 3,  6)  -> rank:  2
     *         t in < 6,  9)  -> rank:  3
     *         t >=   9       -> rank:  4
     *         Note that the higher the absolute value of tStat (rank) the better the tStat.
     */
    public static short getTStatRank(float t) {
        if (t <= -9) {
            return -4;
        } else if (t <= -6) {
            return -3;
        } else if (t <= -3) {
            return -2;
        } else if (t < 0) {
            return -1;
        } else if (t == 0) {
            return 0;
        } else if (t < 3) {
            return 1;
        } else if (t < 6) {
            return 2;
        } else if (t < 9) {
            return 3;
        } else {
            return 4;
        }
    }
}

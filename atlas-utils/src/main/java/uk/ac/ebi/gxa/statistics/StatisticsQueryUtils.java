package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    public static final boolean EFO = true;

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

        // TreeMap used to maintain ordering of processing of experiments in multi-Attribute, multi-Experiment bit index queries to
        // retrieve sorted lists of experiments to be plotted on the gene page.
        Map<Integer, Set<Integer>> allExpsToAttrs = new TreeMap<Integer, Set<Integer>>();

        StatisticsType statType = null;

        for (Attribute attr : orAttributes) {
            if (statType == null) {
                // All clauses of OR queries share the same statisticsType, hence we only
                // need to retrieve it once.
                statType = attr.getStatType();
            }

            if (attr.isEfo() == EFO) {
                String efoTerm = attr.getValue();
                getConditionsForEfo(efoTerm, statisticsStorage, allExpsToAttrs);

            } else { // ef-efv
                StatisticsQueryCondition cond = new StatisticsQueryCondition(attr.getStatType());
                Integer attributeIdx = statisticsStorage.getIndexForAttribute(attr);
                if (attributeIdx != null) {
                    cond.inAttribute(attr);
                    orConditions.orCondition(cond);
                } else {
                    // TODO NB. This is currently possible as sample properties are not currently stored in statisticsStorage
                    log.debug("Attribute " + attr + " was not found in Attribute Index");
                }
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
                attr.setStatType(statType);
                cond.inAttribute(attr);
            }
            orConditions.orCondition(cond);
        }

        return orConditions;
    }

    /**
     * @param efoTerm
     * @param statisticsStorage - used to obtain indexes of attributes and experiments, needed finding experiment counts in bit index
     * @param allExpsToAttrs    Map: experiment index -> Set<Attribute Index> to which mappings for efoterm are to be added
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
     * @param orConditions      StatisticsQueryOrConditions<StatisticsQueryCondition>
     * @param statisticsStorage
     * @param scoringExps       Set of experiments that have at least one non-zero score for statisticsQuery. This is used retrieving efos
     *                          to be displayed in heatmap when no query efvs exist (c.f. atlasStatisticsQueryService.getScoringAttributesForGenes())
     * @return Multiset<Integer> containing experiment counts corresponding to all attribute indexes in each StatisticsQueryCondition in orConditions
     */
    private static Multiset<Integer> getScoresForOrConditions(
            StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions,
            StatisticsStorage statisticsStorage,
            Set<Experiment> scoringExps) {

        Multiset<Integer> scores = HashMultiset.create();
        for (StatisticsQueryCondition orCondition : orConditions.getConditions()) {
            orCondition.setGeneRestrictionSet(orConditions.getGeneRestrictionSet());
            scores.addAll(scoreQuery(orCondition, statisticsStorage, scoringExps));
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
            efoAttrs.add(new Attribute(efo, EFO, statType));
        }
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(statType);
        statsQuery.and(getStatisticsOrQuery(efoAttrs, statisticsStorage));
        Multiset<Integer> counts = getExperimentCounts(statsQuery, statisticsStorage, null);
        return counts;
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
     * @param geneIds
     * @param statType
     * @param statisticsStorage
     * @return Set of efo and efv attributes that have non-zero experiment counts in bit index for at least one of geneIds and statType
     */
    public static Set<Attribute> getScoringAttributesForGenes(Set<Long> geneIds, StatisticsType statType, StatisticsStorage statisticsStorage) {
        long timeStart = System.currentTimeMillis();

        Set<Attribute> result = new HashSet<Attribute>();
        Set<Attribute> allEfvAttributesForStat = statisticsStorage.getAllAttributes(statType);
        for (Attribute attr : allEfvAttributesForStat) {
            attr.setStatType(statType);
            StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(geneIds);
            statsQuery.and(getStatisticsOrQuery(Collections.singletonList(attr), statisticsStorage));
            Set<Experiment> scoringExps = new HashSet<Experiment>();
            StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, scoringExps);
            if (scoringExps.size() > 0) { // at least one gene in geneIds had an experiment count > 0 for attr
                result.add(attr);
                for (Experiment exp : scoringExps) {
                    String efoTerm = statisticsStorage.getEfoTerm(attr, exp);
                    if (efoTerm != null) {
                        result.add(new Attribute(efoTerm, StatisticsQueryUtils.EFO, statType));
                        log.debug("Adding efo: " + efoTerm + " for attr: " + attr + " and exp: " + exp);
                    }
                }
            }
        }
        log.info("Retrieved " + result.size() + " scoring attributes for statType: " + statType + " and gene ids: (" + geneIds + ") in " + (System.currentTimeMillis() - timeStart) + "ms");
        return result;
    }

    /**
     * @param geneId
     * @param statType
     * @param ef
     * @param efv
     * @param statisticsStorage
     * @return Set of Experiments in which geneId-ef-efv have statType expression
     */
    public static Set<Experiment> getScoringExperimentsForGeneAndAttribute(Long geneId, StatisticsType statType, @Nonnull String ef, @Nullable String efv, StatisticsStorage statisticsStorage) {
        Attribute attr = new Attribute(ef, efv);
        attr.setStatType(statType);
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(Collections.singleton(geneId));
        statsQuery.and(getStatisticsOrQuery(Collections.singletonList(attr), statisticsStorage));
        Set<Experiment> scoringExps = new HashSet<Experiment>();
        StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, scoringExps);
        return scoringExps;
    }

    /**
     * If exp cannot be found in exps, add it to exps
     * If it can be found and its pVal/tStat ranks are worse the one is exps, replace it in exps
     *
     * @param exp
     * @param exps
     */
    private static void addOrReplaceExp(Experiment exp, List<Experiment> exps) {
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
     * Populate bestExperimentsSoFar with an (unsorted) list of experiments with best pval/tstat rank, for statisticsQuery
     *
     * @param statisticsQuery
     * @param statisticsStorage
     * @param bestExperimentsSoFar
     */
    public static void getBestExperiments(
            StatisticsQueryCondition statisticsQuery,
            final StatisticsStorage statisticsStorage,
            List<Experiment> bestExperimentsSoFar) {
        Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andStatisticsQueryConditions = statisticsQuery.getConditions();


        if (andStatisticsQueryConditions.isEmpty()) { // End of recursion
            ConciseSet geneRestrictionIdxs = null;
            if (statisticsQuery.getGeneRestrictionSet() != null) {
                geneRestrictionIdxs = statisticsStorage.getIndexesForGeneIds(statisticsQuery.getGeneRestrictionSet());
            }

            Set<Attribute> attributes = statisticsQuery.getAttributes();
            if (!attributes.isEmpty()) {
                setQueryExperiments(statisticsQuery, statisticsStorage);

                // For each experiment in the query, traverse through all query attributes find the best pValue/tStat rank combination
                // for that experiment
                for (Experiment exp : statisticsQuery.getExperiments()) {
                    Integer expIdx = statisticsStorage.getIndexForExperiment(exp);

                    for (Attribute attr : attributes) {
                        Integer attrIdx = statisticsStorage.getIndexForAttribute(attr);

                        SortedMap<PvalTstatRank, Map<Integer, ConciseSet>> pValToExpToGenes =
                                statisticsStorage.getPvalsTStatRanksForAttribute(attrIdx, statisticsQuery.getStatisticsType());

                        if (pValToExpToGenes != null) {
                            for (PvalTstatRank pValTStatRank : pValToExpToGenes.keySet()) {
                                // Since pValToExpToGenes's keySet() is a SortedSet, we traverse key set from better pVal/tStat (lower pVal/higher absolute
                                // value of tStat rank) to worse pVal/tStat (higher pVal, lower absolute valu eof tStat rank)
                                Map<Integer, ConciseSet> expToGenes = pValToExpToGenes.get(pValTStatRank);
                                if (expToGenes != null && expToGenes.get(expIdx) != null &&
                                        // If best experiments are collected for an (OR) group of genes, pVal/tStat
                                        // for any of these genes will be considered here
                                        containsAtLeastOne(expToGenes.get(expIdx), geneRestrictionIdxs)) {
                                    Experiment expCandidate = new Experiment(exp.getAccession(), exp.getExperimentId());
                                    expCandidate.setPvalTstatRank(pValTStatRank);
                                    expCandidate.setHighestRankAttribute(attr);
                                    addOrReplaceExp(expCandidate, bestExperimentsSoFar);
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
}

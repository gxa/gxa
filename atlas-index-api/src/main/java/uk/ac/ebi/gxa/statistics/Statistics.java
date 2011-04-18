package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;

import java.io.Serializable;
import java.util.*;

/**
 * This class stores the following information:
 * **** A. Statistics for Integer BioEntity ids
 * <p/>
 * <p/>
 * Attribute1 index --->   be1 be2...
 * Experiment1 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * Experiment2 index ---> [0, 1, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * Experiment3 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * <p/>
 * Attribute2 index --->
 * Experiment1 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * Experiment2 index ---> [0, 1, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * Experiment3 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * <p/>
 * ...
 * <p/>
 * <p/>
 * NB. Experiment and Attribute indexes point to Experiments and Attributes respectively) via ObjectIndex class
 * <p/>
 * **** B. Pre-computed (Multiset) scores for all bioentities, across all efos. These scores are used
 * to order bioentities in user queries containing no efv/efo conditions.
 * <p/>
 * <p/>
 * **** C. Minimum pValues (rounded to three decimal places) and tStat ranks for each Attribute-Experiment combination:
 * <p/>
 * <p/>
 * Attribute1 index --->
 * pValue/tStat rank --->  be1 be2...
 * Experiment1 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * ...
 * <p/>
 * ...
 * **** D. ef-only Attribute indexes -> ConciseSet of BioEntity ids
 * This is a condensed version (across all experiments) of Statistics (cf. A.) object, just for Ef-only Attributes. It serves
 * to speed up finding of experiment counts for each experiments factor on gene page - by narrowing down the set of experimental
 * factors before searching (and counting of) experiments for each factor for a given bioentity.
 * <p/>
 * <p/>
 * **** E. Ef-efv Attribute index -> ConciseSet of BioEntity ids with up down expressions for ef-efv
 * This is a slightly less condensed version of D., needed for constructing heatmaps on the gene page as well as
 * Efv autocomplete functionality.
 * <p/>
 * <p/>
 * **** F. A Set of all Experiment ids with expression represented by this object
 * <p/>
 * <p/>
 * **** G. Mapping of experiments to bio entities with expression (in the corresponding experiment) represented by this object
 */
public class Statistics implements Serializable {

    private static final long serialVersionUID = 2823036774759163624L;

    // Attribute index -> Experiment index -> ConciseSet of BioEntity ids (See class description A. for more information)
    private Map<Integer, Map<Integer, ConciseSet>> statistics = new HashMap<Integer, Map<Integer, ConciseSet>>();

    // Pre-computed (Multiset) scores for all bio entities, across all efos. These scores are used
    // to order bio entities in user queries containing no efv/efo conditions.
    private Multiset<Integer> scoresAcrossAllEfos = HashMultiset.create();

    /**
     * Attribute index -> pValue/tStat rank -> Experiment index -> ConciseSet of BioEntity ids (See class description for
     * more information). Note that at the level of pValue/tStat ranks the map is sorted in best first order - this will
     * help in ranking experiments w.r.t. to a bioentity-ef-efv triple by lowest pValue/highest absolute value of tStat rank first.
     */
    private Map<Integer, SortedMap<PvalTstatRank, Map<Integer, ConciseSet>>> pValuesTStatRanks =
            new HashMap<Integer, SortedMap<PvalTstatRank, Map<Integer, ConciseSet>>>();

    // ef-only Attribute index -> ConciseSet of BioEntity ids (See class description D. for more information)
    // TreeMap is used to always return ef keySet() in the same order - important for maintaining consistent ordering of experiment lists
    // returned by atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank() - in cases when many experiments share
    // the same pVal/tStatRank
    private Map<Integer, ConciseSet> efAttributeToBioEntities = new TreeMap<Integer, ConciseSet>();

    // Ef-efv Attribute index -> ConciseSet of BioEntity ids with up down expressions for ef-efv (See class description E. for more information)
    private Map<Integer, ConciseSet> efvAttributeToBioEntities = new HashMap<Integer, ConciseSet>();

    // Set of all Experiment ids with expression represented by this object
    private Set<Integer> scoringExperiments = new HashSet<Integer>();

    synchronized
    public void addStatistics(final Integer attributeIndex,
                              final Integer experimentIndex,
                              final Collection<Integer> bioEntityIds) {

        Map<Integer, ConciseSet> stats;

        if (statistics.containsKey(attributeIndex)) {
            stats = statistics.get(attributeIndex);
        } else {
            stats = new HashMap<Integer, ConciseSet>();
            statistics.put(attributeIndex, stats);
        }

        if (stats.containsKey(experimentIndex))
            stats.get(experimentIndex).addAll(bioEntityIds);
        else
            stats.put(experimentIndex, new ConciseSet(bioEntityIds));

        // Store experiment as scoring for StatisticsType represented by this object
        scoringExperiments.add(experimentIndex);
    }

    /**
     * Add bioEntityIds to efAttributeToBioEntities for attributeIndex key
     *
     * @param attributeIndex
     * @param bioEntityIds
     */
    synchronized
    public void addBioEntitiesForEfAttribute(final Integer attributeIndex,
                                       final Collection<Integer> bioEntityIds) {

        if (!efAttributeToBioEntities.containsKey(attributeIndex)) {
            efAttributeToBioEntities.put(attributeIndex, new ConciseSet(bioEntityIds));
        } else {
            efAttributeToBioEntities.get(attributeIndex).addAll(bioEntityIds);
        }
    }

    /**
     * Add geneIndexes to efvAttributeToBioEntities for attributeIndex key
     *
     * @param attributeIndex
     * @param bioEntityIds
     */
    synchronized
    public void addBioEntitiesForEfvAttribute(final Integer attributeIndex,
                                        final Collection<Integer> bioEntityIds) {

        if (!efvAttributeToBioEntities.containsKey(attributeIndex)) {
            efvAttributeToBioEntities.put(attributeIndex, new ConciseSet(bioEntityIds));
        } else {
            efvAttributeToBioEntities.get(attributeIndex).addAll(bioEntityIds);
        }
    }


    /**
     * @param bioEntityId
     * @return Set of Ef-only Attribute indexes that have non-zero up/down experiment counts for geneIdx
     */
    public Set<Integer> getScoringEfAttributesForBioEntity(final Integer bioEntityId) {
        // LinkedHashSet is used to preserve order of entry - important for maintaining consistent ordering of experiment lists
        // returned by atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank() - in cases when many experiments share
        // tha same pVal/tStatRank
        Set<Integer> scoringEfs = new LinkedHashSet<Integer>();
        for (Integer efAttrIndex : efAttributeToBioEntities.keySet()) {
            if (efAttributeToBioEntities.get(efAttrIndex).contains(bioEntityId)) {
                scoringEfs.add(efAttrIndex);
            }
        }
        return scoringEfs;
    }

    /**
     * @param bioEntityId
     * @return Set of Ef-rfv Attribute indexes that have non-zero up/down experiment counts for bioEntityId
     */
    public Set<Integer> getScoringEfvAttributesForBioEntity(final Integer bioEntityId) {
        Set<Integer> scoringEfvs = new HashSet<Integer>();
        for (Integer efAttrIndex : efvAttributeToBioEntities.keySet()) {
            if (efvAttributeToBioEntities.get(efAttrIndex).contains(bioEntityId)) {
                scoringEfvs.add(efAttrIndex);
            }
        }
        return scoringEfvs;
    }

    /**
     * @param attributeIndex
     * @return the amount of bioentities with expression represented by this object for attribute
     */
    public int getBioEntityCountForAttribute(Integer attributeIndex) {
        int bioEntityCount = 0;
        if (efvAttributeToBioEntities.containsKey(attributeIndex))
            bioEntityCount = efvAttributeToBioEntities.get(attributeIndex).size();
        return bioEntityCount;
    }

    public Map<Integer, ConciseSet> getStatisticsForAttribute(Integer attributeIndex) {
        return statistics.get(attributeIndex);
    }

    /**
     * @param attributeIndex
     * @param bioEntityId
     * @return Set of indexes of experiments with non-zero counts for attributeIndex-bioEntityId tuple
     */
    public Set<Integer> getExperimentsForBioEntityAndAttribute(Integer attributeIndex, Integer bioEntityId) {
        Set<Integer> scoringEfsForBioEntities;
        if (attributeIndex != null)
            scoringEfsForBioEntities = Collections.singleton(attributeIndex);
        else
            scoringEfsForBioEntities = getScoringEfAttributesForBioEntity(bioEntityId);

        Set<Integer> expsForBioEntity = new HashSet<Integer>();
        for (Integer attrIndex : scoringEfsForBioEntities) {
            Map<Integer, ConciseSet> expToBioEntities = statistics.get(attrIndex);
            for (Map.Entry<Integer, ConciseSet> expToBioEntity : expToBioEntities.entrySet()) {
                if (expToBioEntity.getValue().contains(bioEntityId)) {
                    expsForBioEntity.add(expToBioEntity.getKey());
                }
            }
        }
        return expsForBioEntity;
    }


    /**
     * @param attributeIndex
     * @return pValue/tStat rank -> Experiment index -> ConciseSet of bioEntityId, corresponding to attributeIndex
     */
    public SortedMap<PvalTstatRank, Map<Integer, ConciseSet>> getPvalsTStatRanksForAttribute(Integer attributeIndex) {
        return pValuesTStatRanks.get(attributeIndex);
    }


    /**
     * @return Scores (experiment counts) across all efo terms
     */
    public Multiset<Integer> getScoresAcrossAllEfos() {
        return scoresAcrossAllEfos;
    }

    public void setScoresAcrossAllEfos(Multiset<Integer> scores) {
        scoresAcrossAllEfos = scores;
    }

    /**
     * @return Set of indexes of All Attributes for which scores exist in this class
     */
    public Set<Integer> getAttributes() {
        return statistics.keySet();
    }

    /**
     * Add pValue/tstat ranks for attribute-experiment-bioentity combination
     *
     * @param attributeIndex
     * @param pValue
     * @param tStatRank
     * @param experimentIndex
     * @param bioEntityId
     */
    synchronized
    public void addPvalueTstatRank(final Integer attributeIndex,
                                   final Float pValue,
                                   final Short tStatRank,
                                   final Integer experimentIndex,
                                   final Integer bioEntityId) {

        SortedMap<PvalTstatRank, Map<Integer, ConciseSet>> pValTStatRankToExpToBioEntities;

        PvalTstatRank pvalTstatRank = new PvalTstatRank(pValue, tStatRank);

        if (pValuesTStatRanks.containsKey(attributeIndex)) {
            pValTStatRankToExpToBioEntities = pValuesTStatRanks.get(attributeIndex);
        } else {
            pValTStatRankToExpToBioEntities = new TreeMap<PvalTstatRank, Map<Integer, ConciseSet>>();
        }

        if (!pValTStatRankToExpToBioEntities.containsKey(pvalTstatRank)) {
            pValTStatRankToExpToBioEntities.put(pvalTstatRank, new HashMap<Integer, ConciseSet>());
        }
        if (!pValTStatRankToExpToBioEntities.get(pvalTstatRank).containsKey(experimentIndex)) {
            pValTStatRankToExpToBioEntities.get(pvalTstatRank).put(experimentIndex, new ConciseSet(bioEntityId));
        } else {
            pValTStatRankToExpToBioEntities.get(pvalTstatRank).get(experimentIndex).add(bioEntityId);
        }

        pValuesTStatRanks.put(attributeIndex, pValTStatRankToExpToBioEntities);
    }

    /**
     * @return Set of all Experiment ids with expression represented by this object
     */
    public Set<Integer> getScoringExperiments() {
        return scoringExperiments;
    }
}


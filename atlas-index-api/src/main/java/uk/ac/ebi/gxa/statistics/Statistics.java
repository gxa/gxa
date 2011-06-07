package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.Multiset;
import com.sun.istack.internal.NotNull;
import it.uniroma3.mat.extendedset.ConciseSet;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.*;

import static com.google.common.collect.HashMultiset.create;
import static com.google.common.collect.Maps.*;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Collections.unmodifiableSet;

/**
 * This class stores the following information:
 * **** A. Statistics for Integer BioEntity ids
 * <p/>
 * <p/>
 * Attribute1 --->   be1 be2...
 * Experiment1 ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * Experiment2 ---> [0, 1, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * Experiment3 ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * <p/>
 * Attribute2 --->
 * Experiment1 ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * Experiment2 ---> [0, 1, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * Experiment3 ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * <p/>
 * ...
 * <p/>
 * <p/>
 * **** B. Pre-computed (Multiset) scores for all bioentities, across all efos. These scores are used
 * to order bioentities in user queries containing no efv/efo conditions.
 * <p/>
 * <p/>
 * **** C. Minimum pValues (rounded to three decimal places) and tStat ranks for each Attribute-Experiment combination:
 * <p/>
 * <p/>
 * Attribute1 --->
 * pValue/tStat rank --->  be1 be2...
 * Experiment1 ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for BioEntity ids)
 * ...
 * <p/>
 * ...
 * **** D. ef-only Attribute -> ConciseSet of BioEntity ids
 * This is a condensed version (across all experiments) of Statistics (cf. A.) object, just for Ef-only Attributes. It serves
 * to speed up finding of experiment counts for each experiments factor on gene page - by narrowing down the set of experimental
 * factors before searching (and counting of) experiments for each factor for a given bioentity.
 * <p/>
 * <p/>
 * **** E. Ef-efv Attribute -> ConciseSet of BioEntity ids with up down expressions for ef-efv
 * This is a slightly less condensed version of D., needed for constructing heatmaps on the gene page as well as
 * Efv autocomplete functionality.
 * <p/>
 * <p/>
 * **** F. A Set of all Experiment ids with expression represented by this object
 * <p/>
 * <p/>
 * **** G. Mapping of experiments to bio entities with expression (in the corresponding experiment) represented by this object
 */
@NotThreadSafe
public class Statistics implements Serializable, StatisticsBuilder {
    private static final long serialVersionUID = 201106061720L;

    // Attribute -> Experiment -> ConciseSet of BioEntity ids (See class description A. for more information)
    private Map<EfvAttribute, Map<ExperimentInfo, ConciseSet>> statistics = newHashMap();

    // Pre-computed (Multiset) scores for all bio entities, across all efos. These scores are used
    // to order bio entities in user queries containing no efv/efo conditions.
    private Multiset<Integer> scoresAcrossAllEfos = create();

    /**
     * Attribute -> pValue/tStat rank -> Experiment -> ConciseSet of BioEntity ids (See class description for
     * more information). Note that at the level of pValue/tStat ranks the map is sorted in best first order - this will
     * help in ranking experiments w.r.t. to a bioentity-ef-efv triple by lowest pValue/highest absolute value of tStat rank first.
     */
    private Map<EfvAttribute, SortedMap<PTRank, Map<ExperimentInfo, ConciseSet>>> pValuesTStatRanks = newHashMap();

    // ef-only Attribute -> ConciseSet of BioEntity ids (See class description D. for more information)
    // LinkedHashMap is used to always return ef keySet() in the same order - important for maintaining consistent ordering of experiment lists
    // returned by atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank() - in cases when many experiments share
    // the same pVal/tStatRank
    private Map<EfvAttribute, ConciseSet> efAttributeToBioEntities = newLinkedHashMap();

    // Ef-efv Attribute -> ConciseSet of BioEntity ids with up down expressions for ef-efv (See class description E. for more information)
    private Map<EfvAttribute, ConciseSet> efvAttributeToBioEntities = newHashMap();

    // Set of all Experiment ids with expression represented by this object
    private Set<ExperimentInfo> scoringExperiments = newHashSet();

    public void addAll(Statistics other) {
        mergeMMC(statistics, other.statistics);
        scoresAcrossAllEfos.addAll(other.scoresAcrossAllEfos);
        mergeMMMC(pValuesTStatRanks, other.pValuesTStatRanks);
        mergeMC(efAttributeToBioEntities, other.efAttributeToBioEntities);
        mergeMC(efvAttributeToBioEntities, other.efvAttributeToBioEntities);
        scoringExperiments.addAll(other.scoringExperiments);
    }

    /**
     * the weird "MMMC" name stands for "merge (Map of Map of Map of ConciseSet)".
     * The only reason we need such a name is Java's type erasure:
     * <code>merge(Map<T, M> a, Map<T, M> b)</code>,
     * <code>merge(Map<T, Map<V, ConciseSet>> a, Map<T, Map<V, ConciseSet>> b)</code>,
     * and <code>merge(Map<T, ConciseSet> a, Map<T, ConciseSet> b)</code>
     * as we'd all prefer to call these would all be just <code>merge(Map, Map)</code>.
     *
     * @param a   map to merge data into
     * @param b   data to merge into a
     * @param <T> Type of a first level key
     * @param <V> Type of a second level key
     * @param <D> Type of a third level key
     * @param <M> Technical parameter, not used externally.
     */
    private <T, V, D, M extends Map<V, Map<D, ConciseSet>>> void mergeMMMC(Map<T, M> a, Map<T, M> b) {
        for (Map.Entry<T, M> entry : b.entrySet()) {
            if (a.containsKey(entry.getKey())) {
                mergeMMC(a.get(entry.getKey()), entry.getValue());
            } else {
                a.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private <T, V> void mergeMMC(Map<T, Map<V, ConciseSet>> a, Map<T, Map<V, ConciseSet>> b) {
        for (Map.Entry<T, Map<V, ConciseSet>> entry : b.entrySet()) {
            if (a.containsKey(entry.getKey())) {
                mergeMC(a.get(entry.getKey()), entry.getValue());
            } else {
                a.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private <T> void mergeMC(Map<T, ConciseSet> a, Map<T, ConciseSet> b) {
        for (Map.Entry<T, ConciseSet> entry : b.entrySet()) {
            if (a.containsKey(entry.getKey())) {
                a.get(entry.getKey()).addAll(entry.getValue());
            } else {
                a.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void addStatistics(@NotNull final EfvAttribute attribute,
                              final ExperimentInfo experiment,
                              final Collection<Integer> bioEntityIds) {

        Map<ExperimentInfo, ConciseSet> attributeStats = statistics.get(attribute);
        if (attributeStats == null) {
            statistics.put(attribute, attributeStats = newHashMap());
        }

        final ConciseSet experimentBioEntities = attributeStats.get(experiment);
        if (experimentBioEntities == null) {
            attributeStats.put(experiment, new ConciseSet(bioEntityIds));
        } else {
            experimentBioEntities.addAll(bioEntityIds);
        }

        // Store experiment as scoring for StatisticsType represented by this object
        scoringExperiments.add(experiment);
    }

    /**
     * Add bioEntityIds to efAttributeToBioEntities for attribute key
     *
     * @param attribute
     * @param bioEntityIds
     */
    @Override
    public void addBioEntitiesForEfAttribute(final EfvAttribute attribute,
                                             final Collection<Integer> bioEntityIds) {

        final ConciseSet efBioEntities = efAttributeToBioEntities.get(attribute);
        if (efBioEntities == null) {
            efAttributeToBioEntities.put(attribute, new ConciseSet(bioEntityIds));
        } else {
            efBioEntities.addAll(bioEntityIds);
        }
    }

    /**
     * Add geneIndexes to efvAttributeToBioEntities for attribute key
     *
     * @param attribute
     * @param bioEntityIds
     */
    @Override
    public void addBioEntitiesForEfvAttribute(final EfvAttribute attribute,
                                              final Collection<Integer> bioEntityIds) {

        final ConciseSet efvBioEntities = efvAttributeToBioEntities.get(attribute);
        if (efvBioEntities == null) {
            efvAttributeToBioEntities.put(attribute, new ConciseSet(bioEntityIds));
        } else {
            efvBioEntities.addAll(bioEntityIds);
        }
    }


    /**
     * @param bioEntityId
     * @return Set of Ef-only Attribute indexes that have non-zero up/down experiment counts for geneIdx
     */
    public Set<EfvAttribute> getScoringEfAttributesForBioEntity(final Integer bioEntityId) {
        // LinkedHashSet is used to preserve order of entry - important for maintaining consistent ordering of experiment lists
        // returned by atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank() - in cases when many experiments share
        // tha same pVal/tStatRank
        final Set<EfvAttribute> scoringEfs = newLinkedHashSet();
        for (Map.Entry<EfvAttribute, ConciseSet> entry : efAttributeToBioEntities.entrySet()) {
            if (entry.getValue().contains(bioEntityId)) {
                scoringEfs.add(entry.getKey());
            }
        }
        return scoringEfs;
    }

    /**
     * @param bioEntityId
     * @return Set of Ef-rfv Attribute indexes that have non-zero up/down experiment counts for bioEntityId
     */
    public Set<EfvAttribute> getScoringEfvAttributesForBioEntity(final Integer bioEntityId) {
        final Set<EfvAttribute> scoringEfvs = newHashSet();
        for (Map.Entry<EfvAttribute, ConciseSet> entry : efvAttributeToBioEntities.entrySet()) {
            if (entry.getValue().contains(bioEntityId)) {
                scoringEfvs.add(entry.getKey());
            }
        }
        return scoringEfvs;
    }

    /**
     * @param attribute
     * @return the amount of bioentities with expression represented by this object for attribute
     */
    public int getBioEntityCountForAttribute(EfvAttribute attribute) {
        ConciseSet bioEntities = efvAttributeToBioEntities.get(attribute);
        return bioEntities == null ? 0 : bioEntities.size();
    }

    public Map<ExperimentInfo, ConciseSet> getStatisticsForAttribute(EfvAttribute attributeIndex) {
        return statistics.get(attributeIndex);
    }

    /**
     * @param attribute
     * @param bioEntityId
     * @return Set of indexes of experiments with non-zero counts for attribute-bioEntityId tuple
     */
    public Set<ExperimentInfo> getExperimentsForBioEntityAndAttribute(EfvAttribute attribute, Integer bioEntityId) {
        final Set<EfvAttribute> scoringEfsForBioEntities;
        scoringEfsForBioEntities = attribute != null ?
                Collections.singleton(attribute) : getScoringEfAttributesForBioEntity(bioEntityId);

        Set<ExperimentInfo> expsForBioEntity = newHashSet();
        for (EfvAttribute attr : scoringEfsForBioEntities) {
            Map<ExperimentInfo, ConciseSet> expToBioEntities = statistics.get(attr);
            for (Map.Entry<ExperimentInfo, ConciseSet> expToBioEntity : expToBioEntities.entrySet()) {
                if (expToBioEntity.getValue().contains(bioEntityId)) {
                    expsForBioEntity.add(expToBioEntity.getKey());
                }
            }
        }
        return expsForBioEntity;
    }


    /**
     * @param attribute
     * @return pValue/tStat rank -> Experiment -> ConciseSet of bioEntityId, corresponding to attribute
     */
    public SortedMap<PTRank, Map<ExperimentInfo, ConciseSet>> getPvalsTStatRanksForAttribute(EfvAttribute attribute) {
        return pValuesTStatRanks.get(attribute);
    }


    /**
     * @return Scores (experiment counts) across all efo terms
     */
    public Multiset<Integer> getScoresAcrossAllEfos() {
        return scoresAcrossAllEfos;
    }

    @Override
    public void setScoresAcrossAllEfos(Multiset<Integer> scores) {
        scoresAcrossAllEfos = scores;
    }

    /**
     * @return Set of All Attributes for which scores exist in this class
     */
    public Set<EfvAttribute> getAttributes() {
        return statistics.keySet();
    }

    /**
     * Add pValue/tstat ranks for attribute-experiment-bioentity combination
     */
    @Override
    public void addPvalueTstatRank(final EfvAttribute attribute,
                                   final PTRank ptRank,
                                   final ExperimentInfo experiment,
                                   final Integer bioEntityId) {
        SortedMap<PTRank, Map<ExperimentInfo, ConciseSet>> pValTStatRankToExpToBioEntities = pValuesTStatRanks.get(attribute);
        if (pValTStatRankToExpToBioEntities == null) {
            pValuesTStatRanks.put(attribute, pValTStatRankToExpToBioEntities = newTreeMap());
        }
        Map<ExperimentInfo, ConciseSet> experimentToBioEntities = pValTStatRankToExpToBioEntities.get(ptRank);
        if (experimentToBioEntities == null) {
            pValTStatRankToExpToBioEntities.put(ptRank, experimentToBioEntities = newHashMap());
        }
        ConciseSet bioEntities = experimentToBioEntities.get(experiment);
        if (bioEntities == null) {
            experimentToBioEntities.put(experiment, new ConciseSet(bioEntityId));
        } else {
            bioEntities.add(bioEntityId);
        }
    }

    @Override
    public Statistics getStatistics() {
        return this;
    }

    /**
     * @return Set of all ExperimentInfos with expression represented by this object
     */
    public Set<ExperimentInfo> getScoringExperiments() {
        return unmodifiableSet(scoringExperiments);
    }
}


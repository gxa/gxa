package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Oct 26, 2010
 * Time: 5:30:51 PM
 *
 * This class stores the following information:
 * **** A. Statistics for Integer gene indexes (indexed to Gene ids via ObjectIndex class)
 *
 * <p/>
 * Attribute1 index --->      g1 g2 g3 g4
 *    Experiment1 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 *    Experiment2 index ---> [0, 1, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 *    Experiment3 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 * <p/>
 * Attribute2 index --->
 *    Experiment1 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 *    Experiment2 index ---> [0, 1, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 *    Experiment3 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 * <p/>
 * ...
 *
 *
 * NB. Experiment and Attribute indexes point to Experiments and Attributes respectively) via ObjectIndex class
 *
 * **** B. Pre-computed (Multiset) scores for all genes, across all efos. These scores are used
 * to order genes in user queries containing no efv/efo conditions.
 *
 *
 * **** C. Minimum pValues (rounded to three decimal places) and tStat ranks for each Attribute-Experiment combination:
 *
 * <p/>
 * Attribute1 index --->
 *         pValue/tStat rank --->
 *              Experiment1 index --->
 *                           g1 g2 g3 g4
 *                           [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 *              ...
 * <p/>
 * ...
 * **** D. ef-only Attribute indexes -> ConciseSet of gene indexes
 * This is a condensed version (across all experiments) of Statistics (cf. A.) object, just for Ef-only Attributes. It serves
 * to speed up finding of experiment counts for each experiments factor on gene page - by narrowing down the set of experimental
 * factors before searching (and counting of) experiments for each factor for a given gene.
 */

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;


public class Statistics implements Serializable {

    private static final long serialVersionUID = 2985336490281457732L;
    // Attribute index -> Experiment index -> ConciseSet of gene indexes (See class description A. for more information)
    private Map<Integer, Map<Integer, ConciseSet>> statistics = new HashMap<Integer, Map<Integer, ConciseSet>>();

    // ef-only Attribute index -> ConciseSet of gene indexes (See class description D. for more information)
    private Map<Integer, ConciseSet> efAttributeToGenes = new HashMap<Integer, ConciseSet>();

    // Pre-computed (Multiset) scores for all genes, across all efos. These scores are used
    // to order genes in user queries containing no efv/efo conditions.
    private Multiset<Integer> scoresAcrossAllEfos = HashMultiset.create();

    /**
     * Attribute index -> pValue/tStat rank -> Experiment index -> ConciseSet of gene indexes (See class description for
     * more information). Note that at the level of pValue/tStat ranks the map is sorted in best first order - this will
     * help in ranking experiments w.r.t. to a gene-ef-efv triple by lowest pValue/highest absolute value of tStat rank first.
     */
    private Map<Integer, SortedMap<PvalTstatRank, Map<Integer, ConciseSet>>> pValuesTStatRanks =
            new HashMap<Integer, SortedMap<PvalTstatRank, Map<Integer, ConciseSet>>>();

    synchronized
    public void addStatistics(final Integer attributeIndex,
                              final Integer experimentIndex,
                              final Collection<Integer> geneIndexes) {

        Map<Integer, ConciseSet> stats;

        if (statistics.containsKey(attributeIndex)) {
            stats = statistics.get(attributeIndex);
        } else {
            stats = new HashMap<Integer, ConciseSet>();
            statistics.put(attributeIndex, stats);
        }

        if (stats.containsKey(experimentIndex))
            stats.get(experimentIndex).addAll(geneIndexes);
        else
            stats.put(experimentIndex, new ConciseSet(geneIndexes));
    }

    /**
     * Add geneIndexes to efAttributeToGenes for attributeIndex key
     * @param attributeIndex
     * @param geneIndexes
     */
    synchronized
    public void addGenes(final Integer attributeIndex,
                         final Collection<Integer> geneIndexes) {


        if (!efAttributeToGenes.containsKey(attributeIndex)) {
            efAttributeToGenes.put(attributeIndex, new ConciseSet(geneIndexes));
        } else {
            efAttributeToGenes.get(attributeIndex).addAll(geneIndexes);
        }
    }


    /**
     * @param geneIdx
     * @return Set of Ef-only attribute indexes that have non-zero up/down experiment counts for geneIdx
     */
    public Set<Integer> getScoringEfsAttributesForGene(final Integer geneIdx) {
        Set<Integer> scoringEfs = new HashSet<Integer>();
        for (Integer efAttrIndex : efAttributeToGenes.keySet()) {
            if (efAttributeToGenes.get(efAttrIndex).contains(geneIdx)) {
                scoringEfs.add(efAttrIndex);
            }
        }
        return scoringEfs;
    }

    public Map<Integer, ConciseSet> getStatisticsForAttribute(Integer attributeIndex) {
        return statistics.get(attributeIndex);
    }

    /**
     * @param attributeIndex
     * @param geneIndex
     * @return Set of indexes of experiments with non-zero counts for attributeIndex-geneIndex tuple
     */
    public Set<Integer> getExperimentsForGeneAndAttribute(Integer attributeIndex, Integer geneIndex) {
        Set<Integer> scoringEfsForGenes;
        if (attributeIndex != null)
            scoringEfsForGenes = Collections.singleton(attributeIndex);
        else
            scoringEfsForGenes = getScoringEfsAttributesForGene(geneIndex);

        Set<Integer> expsForGene = new HashSet<Integer>();
        for (Integer attrIndex : scoringEfsForGenes) {
            Map<Integer, ConciseSet> expToGenes = statistics.get(attrIndex);
            for (Integer expIdx : expToGenes.keySet()) {
                if (expToGenes.get(expIdx).contains(geneIndex)) {
                    expsForGene.add(expIdx);
                }
            }
        }
        return expsForGene;
    }



    /**
     *
     *
     * @param attributeIndex
     * @return  pValue/tStat rank -> Experiment index -> ConciseSet of gene indexes, corresponding to attributeIndex
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
     * Add pValue/tstat ranks for attribute-experiment-genes combination
     * @param attributeIndex
     * @param pValue
     * @param tStatRank
     * @param experimentIndex
     * @param geneIndex
     */
    synchronized
    public void addPvalueTstatRank(final Integer attributeIndex,
                          final Float pValue,
                          final Short tStatRank,
                          final Integer experimentIndex,
                          final Integer geneIndex) {

        SortedMap<PvalTstatRank, Map<Integer, ConciseSet>> pValTStatRankToExpToGenes;

        PvalTstatRank pvalTstatRank = new PvalTstatRank(pValue, tStatRank);

        if (pValuesTStatRanks.containsKey(attributeIndex)) {
            pValTStatRankToExpToGenes = pValuesTStatRanks.get(attributeIndex);
        } else {
            pValTStatRankToExpToGenes = new TreeMap<PvalTstatRank, Map<Integer, ConciseSet>>();
        }

        if (!pValTStatRankToExpToGenes.containsKey(pvalTstatRank)) {
            pValTStatRankToExpToGenes.put(pvalTstatRank, new HashMap<Integer, ConciseSet>());
        }
        if (!pValTStatRankToExpToGenes.get(pvalTstatRank).containsKey(experimentIndex)) {
            pValTStatRankToExpToGenes.get(pvalTstatRank).put(experimentIndex, new ConciseSet(geneIndex));
        } else {
            pValTStatRankToExpToGenes.get(pvalTstatRank).get(experimentIndex).add(geneIndex);
        }

        pValuesTStatRanks.put(attributeIndex, pValTStatRankToExpToGenes);
    }


}


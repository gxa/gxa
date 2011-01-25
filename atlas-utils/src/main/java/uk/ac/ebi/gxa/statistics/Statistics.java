package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Oct 26, 2010
 * Time: 5:30:51 PM
 *
 * Class stores statistics for Integer gene indexes (indexed to Gene ids via ObjectIndex class)
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
 * NB. Experiment and Attribute indexes point to Experiments and Attributes respectively) via ObjectIndex class
 *
 * This class also stores pre-computed (Multiset) scores for all genes, across all efos. These scores are used
 * to order genes in user queries containing no efv/efo conditions.
 *
 * Finally, this class stores minimum pValues (rounded to three decimal places) and tStat ranks for each Attribute-Experiment combination:
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
 */

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;


public class Statistics implements Serializable {

    // Attribute index -> Experiment index -> ConciseSet of gene indexes (See class description for more information)
    private Map<Integer, Map<Integer, ConciseSet>> statistics =
            new HashMap<Integer, Map<Integer, ConciseSet>>();

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

    public Map<Integer, ConciseSet> getStatisticsForAttribute(Integer attributeIndex) {
        return statistics.get(attributeIndex);
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


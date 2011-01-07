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
 */

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;


public class Statistics implements Serializable {

    // See class description for more information
    private Map<Integer, Map<Integer, ConciseSet>> statistics =
            new HashMap<Integer, Map<Integer, ConciseSet>>();

    // Pre-computed (Multiset) scores for all genes, across all efos. These scores are used
    // to order genes in user queries containing no efv/efo conditions.
    private Multiset<Integer> scoresAcrossAllEfos = HashMultiset.create();

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

    public  Map<Integer, ConciseSet> getStatisticsForAttribute(Integer attributeIndex) {
        return statistics.get(attributeIndex);
    }


    /***
     *
     * @return Scores (experiment counts) across all efo terms
     */
    public Multiset<Integer> getScoresAcrossAllEfos() {
        return scoresAcrossAllEfos;
    }

    public void setScoresAcrossAllEfos(Multiset<Integer> scores) {
        scoresAcrossAllEfos = scores;
    }

    /**
     *
     * @return Set of indexes of All Attributes for which scores exist in this class
     */
    public Set<Integer> getAttributes() {
        return statistics.keySet();
    }

}


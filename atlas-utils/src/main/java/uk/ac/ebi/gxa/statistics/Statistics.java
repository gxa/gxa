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
 */

import it.uniroma3.mat.extendedset.ConciseSet;


public class Statistics implements Serializable {
    private static final long serialVersionUID = -164439988781254870L;

    private Map<Integer, Map<Integer, ConciseSet>> statistics =
            new HashMap<Integer, Map<Integer, ConciseSet>>();

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

    synchronized
    public int getNumStatistics(final Integer attributeIndex,
                                final Integer experimentIndex) {
        if (statistics.containsKey(attributeIndex) &&
                statistics.get(attributeIndex).containsKey(experimentIndex)) {
            return statistics.get(attributeIndex).get(experimentIndex).size();
        }
        return 0;
    }

    public  Map<Integer, ConciseSet> getStatisticsForAttribute(Integer attributeIndex) {
        return statistics.get(attributeIndex);
    }

    public Set<Integer> getAttributeIndexes() {
        return statistics.keySet();
    }    

}


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
 * Attribute1 --->         g1 g2 g3 g4
 *    Experiment1 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 *    Experiment2 index ---> [0, 1, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 *    Experiment3 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 * <p/>
 * Attribute2
 *    Experiment1 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 *    Experiment2 index ---> [0, 1, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 *    Experiment3 index ---> [0, 0, 0, 1, ..., 0, 1, 0, ...] (ConciseSet for genes)
 * <p/>
 * ...
 *
 * NB. Experiment indexes point to Experiments via ObjectIndex class
 */

import it.uniroma3.mat.extendedset.ConciseSet;


public class Statistics implements Serializable {
    private static final long serialVersionUID = -164439988781254870L;

    private Map<Attribute, Map<Integer, ConciseSet>> statistics =
            new HashMap<Attribute, Map<Integer, ConciseSet>>();

    synchronized
    public void addStatistics(final Attribute attribute,
                              final Integer experimentIndex,
                              final Collection<Integer> bits) {

        Map<Integer, ConciseSet> stats;

        if (statistics.containsKey(attribute)) {
            stats = statistics.get(attribute);
        } else {
            stats = new HashMap<Integer, ConciseSet>();
            statistics.put(attribute, stats);
        }

        if (stats.containsKey(experimentIndex))
            stats.get(experimentIndex).addAll(bits);
        else
            stats.put(experimentIndex, new ConciseSet(bits));
    }

    synchronized
    public int getNumStatistics(final Attribute attribute,
                                final Integer experimentIndex) {
        if (statistics.containsKey(attribute) &&
                statistics.get(attribute).containsKey(experimentIndex)) {
            return statistics.get(attribute).get(experimentIndex).size();
        }
        return 0;
    }
}


package uk.ac.ebi.gxa.statistics;

import uk.ac.ebi.gxa.utils.Pair;

import java.io.Serializable;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Nov 3, 2010
 * Time: 12:28:06 PM
 * This class stores a mapping between efo terms and their corresponding ef-efv (i.e. Attribute)-Experiment index combinations
 */
public class EfoIndex implements Serializable {

    private static final long serialVersionUID = 2979023740049679685L;

    private Map<String, Set<Pair<Integer, Integer>>> efoIndex = new HashMap<String, Set<Pair<Integer, Integer>>>();

    public void addMapping(String efoTerm, Integer attributeIndex, Integer experimentIndex) {
        if (!efoIndex.containsKey(efoTerm)) {
            efoIndex.put(efoTerm, new HashSet<Pair<Integer, Integer>>());
        }
        efoIndex.get(efoTerm).add(new Pair<Integer, Integer>(attributeIndex, experimentIndex));
    }

    public Set<Pair<Integer, Integer>> getMappingsForEfo(String efoTerm) {
        return efoIndex.get(efoTerm);
    }
}

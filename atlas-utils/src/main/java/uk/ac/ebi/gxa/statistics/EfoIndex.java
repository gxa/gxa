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
 * Note that Attributes are group per experiment. This facilitates scoring of efo queries against bit index.
 */
public class EfoIndex implements Serializable {

    // efoTerm -> experiment index -> Set<Attribute Index>
    private Map<String, Map<Integer, Set<Integer>>> efoIndex = new HashMap<String, Map<Integer, Set<Integer>>>();

    public void addMapping(String efoTerm, Integer attributeIndex, Integer experimentIndex) {
        if (!efoIndex.containsKey(efoTerm)) {
            Map<Integer, Set<Integer>> expToAttrs = new HashMap<Integer, Set<Integer>>();
            efoIndex.put(efoTerm, expToAttrs);
        }

        if (!efoIndex.get(efoTerm).containsKey(experimentIndex)) {
            Set<Integer> attrs = new HashSet<Integer>();
            efoIndex.get(efoTerm).put(experimentIndex, attrs);
        }
        efoIndex.get(efoTerm).get(experimentIndex).add(attributeIndex);
    }

    public Map<Integer, Set<Integer>> getMappingsForEfo(String efoTerm) {
        return efoIndex.get(efoTerm);
    }

    public Set<String> getEfos() {
        return efoIndex.keySet();
    }
}

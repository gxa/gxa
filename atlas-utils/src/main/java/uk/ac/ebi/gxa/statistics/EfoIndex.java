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
 * Note that Attributes are grouped per experiment. This facilitates scoring of efo queries against bit index.
 */
public class EfoIndex implements Serializable {

    private static final long serialVersionUID = -1000094628265441595L;

    // efoTerm -> experiment index -> Set<Attribute Index>
    private Map<String, Map<Integer, Set<Integer>>> efoIndex = new HashMap<String, Map<Integer, Set<Integer>>>();

    // Attribute -> experiment index -> efoTerm
    private Map<Integer, Map<Integer, String>> efvToEfoIndex = new HashMap<Integer, Map<Integer, String>>();

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

        if (!efvToEfoIndex.containsKey(attributeIndex)) {
            Map<Integer, String> expToEfo = new HashMap<Integer, String>();
            efvToEfoIndex.put(attributeIndex, expToEfo);
        }
        efvToEfoIndex.get(attributeIndex).put(experimentIndex, efoTerm);
    }

    public Map<Integer, Set<Integer>> getMappingsForEfo(String efoTerm) {
        return efoIndex.get(efoTerm);
    }

    /**
     *
     * @param attributeIndex
     * @param expIndex
     * @return an efo term one of whose mapping is an efv referenced by attributeIndex in a an experiment expIndex
     */
    public String getEfoTerm(Integer attributeIndex, Integer expIndex) {
        Map<Integer, String> expToEfo = efvToEfoIndex.get(attributeIndex);
        if (expToEfo != null) {
            return expToEfo.get(expIndex);
        }
        return null;
    }

    /**
     *
     * @return all efo terms stored in this index
     */
    public Set<String> getEfos() {
        return efoIndex.keySet();
    }
}

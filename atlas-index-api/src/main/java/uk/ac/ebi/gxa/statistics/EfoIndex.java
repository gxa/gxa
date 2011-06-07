package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * This class stores a mapping between efo terms and their corresponding ef-efv (i.e. Attribute)-Experiment index combinations
 * Note that Attributes are grouped per experiment. This facilitates scoring of efo queries against bit index.
 */
public class EfoIndex implements Serializable {
    private static final long serialVersionUID = 201106061646L;

    // efoTerm -> experiment index -> Set<Attribute Index>
    private Map<String, Map<ExperimentInfo, Set<EfvAttribute>>> efoIndex = newHashMap();

    // Attribute -> experiment index -> efoTerm
    private Map<EfvAttribute, Map<ExperimentInfo, String>> efvToEfoIndex = newHashMap();

    public void addMapping(String efoTerm, EfvAttribute attributeIndex, ExperimentInfo experimentIndex) {
        if (!efoIndex.containsKey(efoTerm)) {
            Map<ExperimentInfo, Set<EfvAttribute>> expToAttrs = newHashMap();
            efoIndex.put(efoTerm, expToAttrs);
        }

        if (!efoIndex.get(efoTerm).containsKey(experimentIndex)) {
            Set<EfvAttribute> attrs = newHashSet();
            efoIndex.get(efoTerm).put(experimentIndex, attrs);
        }
        efoIndex.get(efoTerm).get(experimentIndex).add(attributeIndex);

        if (!efvToEfoIndex.containsKey(attributeIndex)) {
            Map<ExperimentInfo, String> expToEfo = newHashMap();
            efvToEfoIndex.put(attributeIndex, expToEfo);
        }
        efvToEfoIndex.get(attributeIndex).put(experimentIndex, efoTerm);
    }

    public Map<ExperimentInfo, Set<EfvAttribute>> getMappingsForEfo(String efoTerm) {
        return efoIndex.get(efoTerm);
    }

    /**
     * @param attributeIndex
     * @param expIndex
     * @return an efo term one of whose mapping is an efv referenced by attributeIndex in a an experiment expIndex
     */
    public String getEfoTerm(EfvAttribute attributeIndex, ExperimentInfo expIndex) {
        Map<ExperimentInfo, String> expToEfo = efvToEfoIndex.get(attributeIndex);
        return expToEfo != null ? expToEfo.get(expIndex) : null;
    }

    /**
     * @return all efo terms stored in this index
     */
    public Set<String> getEfos() {
        return efoIndex.keySet();
    }
}

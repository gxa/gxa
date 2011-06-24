package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * This class stores a mapping between efo terms and their corresponding ef-efv (i.e. Attribute)-Experiment combinations
 * Note that Attributes are grouped per experiment. This facilitates scoring of efo queries against bit index.
 */
public class EfoIndex implements Serializable {
    private static final long serialVersionUID = 201106061646L;

    // efoTerm -> experiment -> attributes
    private Map<String, Map<ExperimentInfo, Set<EfvAttribute>>> efoIndex = newHashMap();

    // attribute -> experiment -> efoTerm
    private Map<EfvAttribute, Map<ExperimentInfo, String>> efvToEfoIndex = newHashMap();

    public void addMapping(String efoTerm, EfvAttribute attribute, ExperimentInfo experiment) {
        if (!efoIndex.containsKey(efoTerm)) {
            Map<ExperimentInfo, Set<EfvAttribute>> expToAttrs = newHashMap();
            efoIndex.put(efoTerm, expToAttrs);
        }

        if (!efoIndex.get(efoTerm).containsKey(experiment)) {
            Set<EfvAttribute> attrs = newHashSet();
            efoIndex.get(efoTerm).put(experiment, attrs);
        }
        efoIndex.get(efoTerm).get(experiment).add(attribute);

        if (!efvToEfoIndex.containsKey(attribute)) {
            Map<ExperimentInfo, String> expToEfo = newHashMap();
            efvToEfoIndex.put(attribute, expToEfo);
        }
        efvToEfoIndex.get(attribute).put(experiment, efoTerm);
    }

    public Map<ExperimentInfo, Set<EfvAttribute>> getMappingsForEfo(String efoTerm) {
        return efoIndex.get(efoTerm);
    }

    /**
     * @param attribute  EFV to search EFO term for
     * @param experiment the scope of search
     *        TODO: actually, mapping is assay- or sample-scoped, so searching within experiment is a bad idea
     * @return an efo term one of whose mapping is an efv referenced by attribute in a given experiment
     */
    public String getEfoTerm(EfvAttribute attribute, ExperimentInfo experiment) {
        Map<ExperimentInfo, String> expToEfo = efvToEfoIndex.get(attribute);
        return expToEfo != null ? expToEfo.get(experiment) : null;
    }

    /**
     * @return all efo terms stored in this index
     */
    public Set<String> getEfos() {
        return efoIndex.keySet();
    }
}

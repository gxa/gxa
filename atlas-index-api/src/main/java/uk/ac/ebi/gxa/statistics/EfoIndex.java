package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * This class stores a mapping between efo terms and their corresponding ef-efv (i.e. Attribute)-Experiment combinations
 * Note that Attributes are grouped per experiment. This facilitates scoring of efo queries against bit index.
 */
public class EfoIndex implements Serializable {
    private static final long serialVersionUID = 201110201502L;

    // efoTerm -> experiment -> attributes
    private Map<String, Map<ExperimentInfo, Set<EfvAttribute>>> efoIndex = newHashMap();

    // attribute -> experiment -> efoTerm
    private Map<EfvAttribute, Map<ExperimentInfo, Set<String>>> efvToEfoIndex = newHashMap();

    public void addMapping(String efoTerm, EfvAttribute attribute, ExperimentInfo experiment) {
        Map<ExperimentInfo, Set<EfvAttribute>> expToAttrs = efoIndex.get(efoTerm);
        if (expToAttrs == null) {
            efoIndex.put(efoTerm, expToAttrs = newHashMap());
        }
        Set<EfvAttribute> efvAttributes = expToAttrs.get(experiment);
        if (efvAttributes == null) {
            expToAttrs.put(experiment, efvAttributes = newHashSet());
        }
        efvAttributes.add(attribute);

        Map<ExperimentInfo, Set<String>> experimentToEfos = efvToEfoIndex.get(attribute);
        if (experimentToEfos == null) {
            efvToEfoIndex.put(attribute, experimentToEfos = newHashMap());
        }
        Set<String> efos = experimentToEfos.get(experiment);
        if (efos == null) {
            experimentToEfos.put(experiment, efos = newHashSet());
        }
        efos.add(efoTerm);
    }

    public Map<ExperimentInfo, Set<EfvAttribute>> getMappingsForEfo(String efoTerm) {
        final Map<ExperimentInfo, Set<EfvAttribute>> experimentToEfvs = efoIndex.get(efoTerm);
        return experimentToEfvs == null ? Collections.<ExperimentInfo, Set<EfvAttribute>>emptyMap() : experimentToEfvs;
    }

    /**
     * @param attribute  EFV to search EFO term for
     * @param experiment the scope of search
     *                   TODO: actually, mapping is assay- or sample-scoped, so searching within experiment is a bad idea
     * @return an efo term one of whose mapping is an efv referenced by attribute in a given experiment
     */
    public Set<String> getEfoTerms(EfvAttribute attribute, ExperimentInfo experiment) {
        Map<ExperimentInfo, Set<String>> expToEfo = efvToEfoIndex.get(attribute);
        return expToEfo != null ? expToEfo.get(experiment) : Collections.<String>emptySet();
    }

    /**
     * @return all efo terms stored in this index
     */
    public Set<String> getEfos() {
        return efoIndex.keySet();
    }
}

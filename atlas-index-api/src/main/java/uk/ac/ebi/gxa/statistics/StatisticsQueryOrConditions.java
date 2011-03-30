package uk.ac.ebi.gxa.statistics;

import it.uniroma3.mat.extendedset.ConciseSet;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class represents an OR collection of conditions of type: ConditionType.
 */
public class StatisticsQueryOrConditions<ConditionType> {
     // LinkedHashSet used to maintain ordering of processing of experiments in multi-Attribute, multi-Experiment bit index queries to
     // retrieve sorted lists of experiments to be plotted on the gene page.
    private Set<ConditionType> orConditions = new LinkedHashSet<ConditionType>();

    // Set of gene indexes of interest to which this query is restricted
    private ConciseSet geneRestrictionSet = null;

    // Minimum experiment count restriction for this OR query (default: 1)
    private int minExperiments = 1;

    public int getMinExperiments() {
        return minExperiments;
    }

    public void setMinExperiments(int minExperiments) {
        this.minExperiments = minExperiments;
    }

    /**
     * Constructor
     */
    public StatisticsQueryOrConditions() {
    }

    public void orCondition(ConditionType condition) {
        this.orConditions.add(condition);
    }

    public Set<ConditionType> getConditions() {
        return orConditions;
    }

    public ConciseSet getGeneRestrictionSet() {
        return geneRestrictionSet;
    }

    public void setGeneRestrictionSet(ConciseSet geneRestrictionSet) {
        this.geneRestrictionSet = geneRestrictionSet;
    }

    @Override
    public String toString() {
        return orConditions.toString();
    }

}


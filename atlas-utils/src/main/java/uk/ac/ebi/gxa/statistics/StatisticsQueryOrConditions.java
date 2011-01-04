package uk.ac.ebi.gxa.statistics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Dec 3, 2010
 * Time: 3:27:59 PM
 * This class represents an OR collection of conditions of type: ConditionType.
 */
public class StatisticsQueryOrConditions<ConditionType> {
    private Set<ConditionType> orConditions = new HashSet<ConditionType>();
    // If orConditions represents an OR collection of experiment-efv combinations
    // corresponding to an efo term, efoTerm is used to hold that term's name.
    String efoTerm = null;
    // Set of gene ids of interest to which this query is restricted
    private Set<Long> geneRestrictionSet = null;

    /**
     * Constructor
     */
    public StatisticsQueryOrConditions() {
    }

    public String getEfoTerm() {
        return efoTerm;
    }

    public void setEfoTerm(String efoTerm) {
        this.efoTerm = efoTerm;
    }

    public void orCondition(ConditionType condition) {
        this.orConditions.add(condition);
    }

    public Set<ConditionType> getConditions() {
        return orConditions;
    }

    public Set<Long> getGeneRestrictionSet() {
        return geneRestrictionSet;
    }

    public void setGeneRestrictionSet(Set<Long> geneRestrictionSet) {
        this.geneRestrictionSet = geneRestrictionSet;
    }

    @Override
    public String toString() {
        return orConditions.toString();
    }

}


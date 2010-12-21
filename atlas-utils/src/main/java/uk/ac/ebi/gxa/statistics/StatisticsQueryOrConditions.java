package uk.ac.ebi.gxa.statistics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Dec 3, 2010
 * Time: 3:27:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatisticsQueryOrConditions<ConditionType> {
    private Set<ConditionType> orConditions = new HashSet<ConditionType>();
    String efoTerm = null;
    private Set<Long> geneRestrictionSet = null;

    public StatisticsQueryOrConditions() {
    }

    public StatisticsQueryOrConditions(ConditionType condition) {
        this.orConditions.add(condition);
    }

    public StatisticsQueryOrConditions(Collection<ConditionType> orConditions) {
        this.orConditions.addAll(orConditions);
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

    public Integer getNumOfOrConditions() {
          return orConditions.size();
    }
}


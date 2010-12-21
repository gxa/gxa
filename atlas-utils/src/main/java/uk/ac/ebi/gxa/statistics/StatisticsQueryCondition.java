package uk.ac.ebi.gxa.statistics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Dec 3, 2010
 * Time: 3:27:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatisticsQueryCondition {
    // StatisticsQueryCondition in practice will have either:
    // 1. just andConditions or
    // 2. experiments AND attributes (AND query - with experiments and attributes in themselves being OR queries)
    private Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andConditions = new HashSet<StatisticsQueryOrConditions<StatisticsQueryCondition>>();
    private Set<Experiment> experiments = new HashSet<Experiment>();  // OR set of experiments
    private Set<Attribute> attributes = new HashSet<Attribute>(); // OR set of attributes
    private StatisticsType statisticsType;
    private Set<Long> geneRestrictionSet = null;
    


    private static final String PRETTY_PRINT_OFFSET = "  ";

    public StatisticsQueryCondition() {
    }

    public StatisticsQueryCondition(StatisticsType statisticsType) {
        this.statisticsType = statisticsType;
    }

    public StatisticsQueryCondition(Set<Long> geneRestrictionSet) {
        this.geneRestrictionSet = geneRestrictionSet;
    }

    public void setGeneRestrictionSet(Set<Long> geneRestrictionSet) {
        this.geneRestrictionSet = geneRestrictionSet;
    }

    public Set<Long> getGeneRestrictionSet() {
        return geneRestrictionSet;
    }

    public StatisticsType getStatisticsType() {
        return statisticsType;
    }

    public StatisticsQueryCondition and(StatisticsQueryCondition condition, String efoTerm) {
        StatisticsQueryOrConditions<StatisticsQueryCondition> StatisticsQueryOrConditions = new StatisticsQueryOrConditions<StatisticsQueryCondition>(condition);
        StatisticsQueryOrConditions.setEfoTerm(efoTerm);
        andConditions.add(StatisticsQueryOrConditions);
        return this;
    }

    public StatisticsQueryCondition and(StatisticsQueryOrConditions<StatisticsQueryCondition> statisticsQueryOrConditions) {
        statisticsQueryOrConditions.setGeneRestrictionSet(geneRestrictionSet);
        andConditions.add(statisticsQueryOrConditions);
        return this;
    }

    public StatisticsQueryCondition and(Collection<StatisticsQueryCondition> conditions, String efoTerm) {
        StatisticsQueryOrConditions<StatisticsQueryCondition> statisticsQueryOrConditions = new StatisticsQueryOrConditions<StatisticsQueryCondition>(conditions);
        statisticsQueryOrConditions.setGeneRestrictionSet(geneRestrictionSet);
        statisticsQueryOrConditions.setEfoTerm(efoTerm);
        andConditions.add(statisticsQueryOrConditions);
        return this;
    }

    public Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> getConditions() {
        return andConditions;
    }

    public StatisticsQueryCondition inExperiment(Experiment experiment) {
        experiments.add(experiment);
        return this;
    }

    public StatisticsQueryCondition inExperiments(Collection<Experiment> experiments) {
        this.experiments.addAll(experiments);
        return this;
    }

    public Set<Experiment> getExperiments() {
        return experiments;
    }

    public StatisticsQueryCondition inAttribute(Attribute attribute) {
        this.attributes.add(attribute);
        return this;
    }

    public Set<Attribute> getAttributes() {
        return attributes;
    }

    public String prettyPrint(String offset) {
        StringBuilder sb = new StringBuilder();
        Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andGeneConditions = getConditions();
        if (getGeneRestrictionSet() != null) {
           sb.append("\n(GeneRestrictionSet size = " + getGeneRestrictionSet().size()).append(") "); 
        }
        if (!andGeneConditions.isEmpty()) {
            sb.append("\n").append(offset).append(" [ ");
            int i = 0;
            for (StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions : andGeneConditions) {
                if (orConditions.getEfoTerm() != null) {
                    sb.append(" efo: " + orConditions.getEfoTerm() + " -> ");
                }
                for (StatisticsQueryCondition geneCondition : orConditions.getConditions()) {
                    sb.append(geneCondition.prettyPrint(offset + PRETTY_PRINT_OFFSET));
                }
                if (++i < andGeneConditions.size())
                    sb.append(" AND ");
            }
            sb.append("\n").append(offset).append(" ] ");
        } else { // TODO end of recursion

            Set<Attribute> attrs = getAttributes();
            Set<Experiment> exps = getExperiments();

            // Output attributes
            if (!attrs.isEmpty()) {
                sb.append("in attrs: [ ");
                int i = 0;
                for (Attribute attr : attrs) {
                    sb.append(attr);
                    if (++i < attrs.size())
                        sb.append(" OR ");
                }
                sb.append(" ] ");
                if (!exps.isEmpty()) {
                    sb.append(" AND ");
                }
            }
            // Output experiments

            if (!exps.isEmpty()) {

                sb.append("in exps: [ ");
                int i = 0;
                for (Experiment exp : exps) {
                    sb.append(exp.getAccession());
                    if (++i < attrs.size())
                        sb.append(" OR ");
                }
                sb.append(" ] ");
            }
        }
        return sb.toString();
    }
}

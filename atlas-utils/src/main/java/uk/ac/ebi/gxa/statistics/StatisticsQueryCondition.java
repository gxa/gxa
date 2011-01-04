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
    // 1. just andConditions, or
    // 2. experiments AND attributes (i.e. an AND query - with experiments and attributes in themselves being OR queries)
    private Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andConditions = new HashSet<StatisticsQueryOrConditions<StatisticsQueryCondition>>();
    private Set<Experiment> experiments = new HashSet<Experiment>();  // OR set of experiments
    private Set<Attribute> attributes = new HashSet<Attribute>(); // OR set of attributes
    private StatisticsType statisticsType;
    private Set<Long> geneRestrictionSet = null;


    private static final String INITIAL_PRETTY_PRINT_OFFSET = "";
    private static final String PRETTY_PRINT_OFFSET = "  ";

    /**
     * Constructor
     */
    public StatisticsQueryCondition() {
    }

    /**
     * Constructor
     *
     * @param statisticsType
     */
    public StatisticsQueryCondition(StatisticsType statisticsType) {
        this.statisticsType = statisticsType;
    }

    /**
     * Constructor
     *
     * @param geneRestrictionSet
     */
    public StatisticsQueryCondition(Set<Long> geneRestrictionSet) {
        this.geneRestrictionSet = geneRestrictionSet;
    }

    /**
     * @param geneRestrictionSet Set of gene Ids of interest, to which this query condition should be restricted. 
     */
    public void setGeneRestrictionSet(Set<Long> geneRestrictionSet) {
        this.geneRestrictionSet = geneRestrictionSet;
    }

    public Set<Long> getGeneRestrictionSet() {
        return geneRestrictionSet;
    }

    public StatisticsType getStatisticsType() {
        return statisticsType;
    }

    /**
     *
     * @param statisticsQueryOrConditions
     * @return StatisticsQueryCondition containing an OR clause of statisticsQueryOrConditions, restricted to geneRestrictionSet
     */
    public StatisticsQueryCondition and(StatisticsQueryOrConditions<StatisticsQueryCondition> statisticsQueryOrConditions) {
        statisticsQueryOrConditions.setGeneRestrictionSet(geneRestrictionSet);
        andConditions.add(statisticsQueryOrConditions);
        return this;
    }

    public Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> getConditions() {
        return andConditions;
    }

    /**
     *
     * @param experiments
     * @return this query condition with experiments added to its experiments OR clause
     */
    public StatisticsQueryCondition inExperiments(Collection<Experiment> experiments) {
        this.experiments.addAll(experiments);
        return this;
    }

    public Set<Experiment> getExperiments() {
        return experiments;
    }

    /**
     * 
     * @param attribute
     * @return this query condition with attribute added to its attributes OR clause
     */
    public StatisticsQueryCondition inAttribute(Attribute attribute) {
        this.attributes.add(attribute);
        return this;
    }

    public Set<Attribute> getAttributes() {
        return attributes;
    }


    public String prettyPrint() {
        return prettyPrint(INITIAL_PRETTY_PRINT_OFFSET);
    }

    /**
     * Recursive method to pretty print this StatisticsQueryCondition
     *
     * @param offset
     * @return
     */
    private String prettyPrint(String offset) {
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

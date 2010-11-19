package ae3.service.structuredquery;

import uk.ac.ebi.gxa.statistics.StatisticsType;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Nov 11, 2010
 * Time: 1:42:13 PM
 * Class implementing BitIndex query interface used by the AtlasStatisticsQueryService
 */
public class AtlasBitIndexQueryBuilder {
    public static Where constructQuery() {
        return new Where();
    }

    public static class Where {
        public GeneCondition where(StatisticsType statisticsType) {
            return new GeneCondition(statisticsType);
        }
    }

    public static class GeneCondition {
        // GeneCondition in practice will have either:
        // 1. just andConditions or
        // 2. experiments AND attributes (AND query - with experiments and attributes in themselves being OR queries)
        private Set<OrConditions<GeneCondition>> andConditions = new HashSet<OrConditions<GeneCondition>>();
        private Set<Integer> experiments = new HashSet<Integer>();  // OR set of experiments
        private Set<Integer> attributes = new HashSet<Integer>(); // OR set of attributes
        private StatisticsType statisticsType;

        public GeneCondition(StatisticsType statisticsType) {
            this.statisticsType = statisticsType;
        }

        public StatisticsType getStatisticsType() {
            return statisticsType;
        }

        public GeneCondition and(GeneCondition condition, String efoTerm) {
            OrConditions<GeneCondition> orConditions = new OrConditions<GeneCondition>(condition);
            orConditions.setEfoTerm(efoTerm);
            andConditions.add(orConditions);
            return this;
        }

        public GeneCondition and(OrConditions<GeneCondition> orConditions) {
            andConditions.add(orConditions);
            return this;
        }

        public GeneCondition and(Collection<GeneCondition> conditions, String efoTerm) {
            OrConditions<GeneCondition> orConditions = new OrConditions<GeneCondition>(conditions);
            orConditions.setEfoTerm(efoTerm);
            andConditions.add(orConditions);
            return this;
        }

        public Set<OrConditions<GeneCondition>> getConditions() {
            return andConditions;
        }

        public GeneCondition inExperiment(Integer experiment) {
            experiments.add(experiment);
            return this;
        }

        public GeneCondition inExperiments(Collection<Integer> experiments) {
            this.experiments.addAll(experiments);
            return this;
        }

        public Set<Integer> getExperiments() {
            return experiments;
        }

        public GeneCondition inAttribute(Integer attribute) {
            this.attributes.add(attribute);
            return this;
        }

        public GeneCondition inAttributes(Collection<Integer> attributes) {
            this.attributes.addAll(attributes);
            return this;
        }

        public Set<Integer> getAttributes() {
            return attributes;
        }
    }

    public static class OrConditions<ConditionType> {
        private Set<ConditionType> orConditions = new HashSet<ConditionType>();
        String efoTerm = null;

        public OrConditions() {
        }

        public OrConditions(ConditionType condition) {
            this.orConditions.add(condition);
        }

        public OrConditions(Collection<ConditionType> orConditions) {
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

        @Override
        public String toString() {
            return orConditions.toString();
        }
    }
}
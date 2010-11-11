package ae3.service.structuredquery;

import uk.ac.ebi.gxa.statistics.StatisticsType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
        public AtlasQuery where(GeneCondition geneCondition) {
            return new AtlasQuery(geneCondition);
        }

        public AtlasQuery where(Collection<GeneCondition> geneConditions) {
            return new AtlasQuery(geneConditions);
        }
    }

    private static abstract class Query<ConditionType> {
        protected Set<OrConditions<ConditionType>> conditions = new HashSet<OrConditions<ConditionType>>();

        protected Query(ConditionType condition) {
            and(condition);
        }

        protected Query(Collection<ConditionType> conditions) {
            and(conditions);
        }

        protected Query() {
        }

        private Query and(ConditionType condition) {
            conditions.add(new OrConditions<ConditionType>(condition));
            return this;
        }

        private Query and(Collection<ConditionType> conditions) {
            this.conditions.add(new OrConditions<ConditionType>(conditions));
            return this;
        }

        private Set<OrConditions<ConditionType>> getConditions() {
            return conditions;
        }

    }

    public static class AtlasQuery extends Query<GeneCondition> {
        private Set<Integer> experiments = new HashSet<Integer>();

        public AtlasQuery(GeneCondition geneCondition) {
            super(geneCondition);
        }

        public AtlasQuery(Collection<GeneCondition> geneConditions) {
            super(geneConditions);
        }

        public AtlasQuery and(GeneCondition condition) {
            super.and(condition);
            return this;
        }

        public AtlasQuery and(Collection<GeneCondition> conditions) {
            super.and(conditions);
            return this;
        }

        public AtlasQuery inExperiment(Integer experiment) {
            experiments.add(experiment);
            return this;
        }

        public AtlasQuery inExperiments(Collection<Integer> experiments) {
            this.experiments.addAll(experiments);
            return this;
        }

        public Set<OrConditions<GeneCondition>> getGeneConditions() {
            return super.getConditions();
        }

        public Set<Integer> getExperiments() {
            return experiments;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("AtlasQuery [");

            if (!conditions.isEmpty()) {
                sb.append("find gene = [");

                int count = 0;
                for (OrConditions<GeneCondition> orGeneCond : conditions) {
                    sb.append(orGeneCond.toString());
                    if (++count < conditions.size()) sb.append(" AND ");
                }
                sb.append("]");
            }

            if (!experiments.isEmpty()) {
                sb.append(" in experiments ");
                sb.append(experiments);
            }

            return sb.toString();
        }
    }

    public static GeneCondition geneIs(StatisticsType statisticType) {
        return new GeneCondition(statisticType);
    }

    public static class GeneCondition extends Query<Integer> {
        private StatisticsType statisticType;
        private Set<Integer> experiments = new HashSet<Integer>();

        public GeneCondition(StatisticsType statisticType) {
            this.statisticType = statisticType;
        }

        public GeneCondition inAttribute(Integer attribute) {
            super.and(attribute);
            return this;
        }

        public GeneCondition inAttributes(Collection<Integer> attributes) {
            super.and(attributes);
            return this;
        }

        public GeneCondition inAttributes(Integer... attributes) {
            super.and(Arrays.asList(attributes));
            return this;
        }

        public GeneCondition inExperiment(Integer experiment) {
            experiments.add(experiment);
            return this;
        }

        public GeneCondition inExperiments(Collection<Integer> expts) {
            experiments.addAll(expts);
            return this;
        }

        public Set<OrConditions<Integer>> getAttributeConditions() {
            return super.getConditions();
        }

        public Set<Integer> getExperiments() {
            return experiments;
        }

        public StatisticsType getStatisticType() {
            return statisticType;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(statisticType);

            if (!conditions.isEmpty()) {
                sb.append(" in ");

                int count = 0;
                for (OrConditions<Integer> orAttributeCond : conditions) {
                    sb.append(orAttributeCond.toString());
                    if (++count < conditions.size()) sb.append(" AND ");
                }
            }

            if (!experiments.isEmpty())
                sb.append(" in ").append(experiments);

            return sb.toString();
        }
    }

    public static class OrConditions<ConditionType> {
        private Set<ConditionType> conditions = new HashSet<ConditionType>();

        public OrConditions(ConditionType condition) {
            this.conditions.add(condition);
        }

        public OrConditions(Collection<ConditionType> conditions) {
            this.conditions.addAll(conditions);
        }

        public Set<ConditionType> getConditions() {
            return conditions;
        }

        @Override
        public String toString() {
            return conditions.toString();
        }
    }
}
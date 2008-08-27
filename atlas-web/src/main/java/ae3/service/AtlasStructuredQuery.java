package ae3.service;

import java.util.List;
import java.util.ArrayList;

/**
 * A class, representing gene expression options for extended Atlas query
 * @author pashky
 */
public class AtlasStructuredQuery {
    static public enum Expression {
        UP("up"),
        DOWN("down"),
        NOT_UP("not up"),
        NOT_DOWN("not down"),
        NOT_EXPRESSED("not expressed"),
        UP_DOWN("up or down");

        private String description;
        Expression(String description) { this.description = description; }

        /**
         * Get human-readable option description
         * @return description string
         */
        public String getDescription() { return description; }

        /**
         * Lists all available options and their human-readable representation
         * @return list of gene expression options
         */
        static public List<String[]> getOptionsList() {
            List<String[]> result = new ArrayList<String[]>();
            for(Expression r : values())
            {
               result.add(new String[] { r.name(), r.getDescription() });
            }
            return result;
        }
    }

    /**
     * Class representing one experiment condition
     */
    static public class Condition {
        private Expression expression;
        private String factor;
        private List<String> factorValues;

        public Condition() {
        }

        public Expression getExpression() {
            return expression;
        }

        public void setExpression(Expression expression) {
            this.expression = expression;
        }

        public String getFactor() {
            return factor;
        }

        public void setFactor(String factor) {
            this.factor = factor;
        }

        public List<String> getFactorValues() {
            return factorValues;
        }

        public void setFactorValues(List<String> factorValues) {
            this.factorValues = factorValues;
        }
    }

    private String gene;
    private List<String> species;
    private List<Condition> conditions;

    public AtlasStructuredQuery() {
        conditions = new ArrayList<Condition>();
    }

    public String getGene() {
        return gene;
    }

    public void setGene(String gene) {
        this.gene = gene;
    }

    public List<String> getSpecies() {
        return species;
    }

    public void setSpecies(List<String> species) {
        this.species = species;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public void addCondition(Condition condition) {
        this.conditions.add(condition);
    }
}

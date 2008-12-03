package ae3.service.structuredquery;

import java.util.List;
import java.util.ArrayList;

/**
 * A class, representing gene expression options for extended Atlas query
 * @author pashky
 */
public class AtlasStructuredQuery {
    static public enum Expression {
        UP_DOWN("up or down"),
        UP("up"),
        DOWN("down");

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

        public String getJointFactorValues() {
            StringBuffer sb = new StringBuffer();
            for (String v : factorValues)
            {
                if(sb.length() > 0)
                    sb.append(" ");
                if(v.indexOf(' ') >= 0)
                    sb.append('"').append(v).append('"');
                else
                    sb.append(v);
            }
            return sb.toString();
        }

        public void setFactorValues(List<String> factorValues) {
            this.factorValues = factorValues;
        }

        public boolean isAnyFactor() {
            return getFactor().length() == 0;
        }

        public boolean isAnyValue() {
            for(String v : getFactorValues())
                if(!v.equals("") && !v.equals("*"))
                    return false;
            return true;
        }

        public boolean isAnything() {
            return isAnyValue() && isAnyFactor();
        }

    }

    private String gene;
    private List<String> species;
    private List<Condition> conditions;
    private int start;
    private int rows;

    public AtlasStructuredQuery() {
        conditions = new ArrayList<Condition>();
        start = 0;
        rows = 100;
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

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public boolean isSimple() {
        return conditions.size() == 0 || (conditions.size() == 1 && "".equals(conditions.get(0).getFactor()));
    }
}

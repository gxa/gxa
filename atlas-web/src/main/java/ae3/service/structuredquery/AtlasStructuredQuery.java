package ae3.service.structuredquery;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * Atlas structured query container class for parsed parameters
 * @author pashky
 */
public class AtlasStructuredQuery {
    /**
     * Gene epxression option
     */
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

        /**
         * Returns gene expression type
         * @return gene expression type
         */
        public Expression getExpression() {
            return expression;
        }

        /**
         * Sets gene expression type
         * @param expression gene expression type
         */
        public void setExpression(Expression expression) {
            this.expression = expression;
        }

        /**
         * Returns factor
         * @return factor name
         */
        public String getFactor() {
            return factor;
        }

        /**
         * Sets factor name
         * @param factor factor name
         */
        public void setFactor(String factor) {
            this.factor = factor;
        }

        /**
         * Returns factor values
         * @return iterable factor values
         */
        public List<String> getFactorValues() {
            return factorValues;
        }

        /**
         * Returns string with space-separated factor values, quoted if necessary
         * @return string of all factor values
         */
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

        /**
         * Sets factor values
         * @param factorValues list of factor values
         */
        public void setFactorValues(List<String> factorValues) {
            this.factorValues = factorValues;
        }

        /**
         * Convenience method to check whether conditions is for any factor
         * @return true if any factor
         */
        public boolean isAnyFactor() {
            return getFactor().length() == 0;
        }

        /**
         * Convenience method to check whether conditions is for any value
         * @return true if any value contains '*' or all values are empty
         */
        public boolean isAnyValue() {
            for(String v : getFactorValues())
                if(v.equals("*"))
                    return true;
            for(String v : getFactorValues())
                if(!v.equals(""))
                    return false;
            return true;
        }

        /**
         * Convenience method to check whether condition is for anything (any value and any factor)
         * @return
         */
        public boolean isAnything() {
            return isAnyValue() && isAnyFactor();
        }

    }

    /**
     *Class representing one gene query 
     */
    static public class GeneQuery{
    	private String qry;
    	private String property;
    	private String operator;
    	
    	public GeneQuery(){
    		
    	}

		public String getQry() {
			return qry;
		}

		public void setQry(String qry) {
			this.qry = qry;
		}

		public String getProperty() {
			return property;
		}

		public void setProperty(String property) {
			this.property = property;
		}

		public String getOperator() {
			return operator;
		}

		public void setOperator(String operator) {
			this.operator = operator;
		}
    	
    }
    
    private List<String> species;
    private List<Condition> conditions;
    private List<GeneQuery> geneQueries;
    private int start;
    private int rows;
    private Set<String> expandColumns;

    public AtlasStructuredQuery() {
        conditions = new ArrayList<Condition>();
        geneQueries = new ArrayList<GeneQuery>();
        start = 0;
        rows = 100;
    }

    /**
     * sets lists of gene queries represented by each row added to the query
     * @param geneQueries
     */
    public void setGeneQueries(List<GeneQuery> geneQueries){
    	this.geneQueries = geneQueries;
    }
    
    /**
     * Returns gene queries for the current query. Includes for each query (query, query operator and gene property)
     * @return geneQueries
     */
    public List<GeneQuery> getGeneQueries(){
    	return geneQueries;
    }
    
    /**
     * Returns gene query for the first gene query row (case of simple query)
     * @return gene query
     */
    public String getGene() {
        return geneQueries.get(0).qry;
    }

    /**
     * Returns list of species
     * @return list of species
     */
    public List<String> getSpecies() {
        return species;
    }

    /**
     * Sets list of species
     * @param species list of species
     */
    public void setSpecies(List<String> species) {
        this.species = species;
    }

    /**
     * Returns list of EFV conditions
     * @return list of EFV conditions
     * @see ae3.service.structuredquery.AtlasStructuredQuery.Condition
     */
    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * Sets list of EFV conditions
     * @param conditions list of EFV conditions
     * @see ae3.service.structuredquery.AtlasStructuredQuery.Condition
     */
    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    /**
     * Adds EFV condition ot list
     * @param condition condition to add
     */
    public void addCondition(Condition condition) {
        this.conditions.add(condition);
    }

    /**
     * Returns start position
     * @return start position
     */
    public int getStart() {
        return start;
    }

    /**
     * Sets required start position in paging
     * @param start position in paging
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Returns required number of rows in page
     * @return number of rows in page
     */
    public int getRows() {
        return rows;
    }

    /**
     * Sets required number of rows in page
     * @param rows number of rows in page
     */
    public void setRows(int rows) {
        this.rows = rows;
    }

    /**
     * Checks if query is "simple" query (just one condition)
     * @return
     */
    public boolean isSimple() {
        return (conditions.size() == 0 || (conditions.size() == 1 && "".equals(conditions.get(0).getFactor()))) && (geneQueries.size() ==1 && geneQueries.get(0).property==null);
    }

    /**
     * Returns set of required expanded columns
     * @return set of expanded columns
     */
    public Set<String> getExpandColumns()
    {
        return expandColumns;
    }

    /**
     * Sets required expanded columns
     * @param expandColumns set of expanded columns
     */
    public void setExpandColumns(Set<String> expandColumns) {
        this.expandColumns = expandColumns;
    }
}

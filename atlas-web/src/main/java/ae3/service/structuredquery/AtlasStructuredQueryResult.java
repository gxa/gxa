package ae3.service.structuredquery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Atlas structured query result container class
 * @author pashky
 */
public class AtlasStructuredQueryResult {
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Structured query condition expanded by query service
     */
    public static class Condition  {
        private AtlasStructuredQuery.Condition condition;
        private EfvTree<Boolean> expansion;

        /**
         * Constructor for condition
         * @param condition original query condition
         * @param expansion EFV expansion tree
         */
        public Condition(AtlasStructuredQuery.Condition condition, EfvTree<Boolean> expansion) {
            this.condition = condition;
            this.expansion = expansion;
        }

        /**
         * Returns gene expression type
         * @return gene expression type
         */
        public AtlasStructuredQuery.Expression getExpression() {
            return condition.getExpression();
        }

        /**
         * Returns factor
         * @return factor name
         */
        public String getFactor() {
            return condition.getFactor();
        }

        /**
         * Returns factor values
         * @return iterable factor values
         */
        public Iterable<String> getFactorValues() {
            return condition.getFactorValues();
        }

        /**
         * Return EFV expansion tree
         * @return EFV expansion tree
         */
        public EfvTree<Boolean> getExpansion() {
            return expansion;
        }

        /**
         * Convenience method to check whether conditions is for any factor
         * @return true if any factor
         */
        public boolean isAnyFactor() {
            return condition.isAnyFactor();
        }

        /**
         * Convenience method to check whether conditions is for any value
         * @return true if any value contains '*' or all values are empty
         */
        public boolean isAnyValue() {
            return condition.isAnyValue();
        }

        /**
         * Convenience method to check whether condition is for anything (any value and any factor)
         * @return
         */
        public boolean isAnything() {
            return condition.isAnything();
        }
    }

    private EfvTree<Integer> resultEfvs;
    private Collection<StructuredResultRow> results;
    private Iterable<Condition> conditions;
    private Set<String> expandableEfs;

    private long total;
    private long start;
    private long rows;

    private EfvTree<FacetUpDn> efvFacet;
    private Map<String,Iterable<FacetCounter>> geneFacets;

    /**
     * Constructor
     * @param start starting position in paging
     * @param rows number of rows in page
     */
    public AtlasStructuredQueryResult(long start, long rows) {
        this.results = new ArrayList<StructuredResultRow>();
        this.geneFacets = new HashMap<String, Iterable<FacetCounter>>();
        this.start = start;
        this.rows = rows;
    }

    /**
     * Adds result to list
     * @param result result to add
     */
    public void addResult(StructuredResultRow result) {
        results.add(result);
    }

    /**
     * Returns number of results
     * @return number of results in result list
     */
    public int getSize() {
        return results.size();
    }

    /**
     * Return iterable results
     * @return iterable results
     */
    public Iterable<StructuredResultRow> getResults() {
        return results;
    }

    /**
     * Set results EFVs tree
     * @param resultEfvs result EFVs tree
     */
    public void setResultEfvs(EfvTree<Integer> resultEfvs) {
        this.resultEfvs = resultEfvs;
    }

    /**
     * Returns result EFVs tree
     * @return
     */
    public EfvTree<Integer> getResultEfvs() {
        return resultEfvs;
    }

    /**
     * Returns results start position in paging
     * @return start position
     */
    public long getStart() {
        return start;
    }

    /**
     * Returns total number of results
     * @return total number of results
     */
    public long getTotal() {
        return total;
    }

    /**
     * Returns number of rows in page
     * @return number of rows in page
     */
    public long getRows() {
        return rows;
    }

    /**
     * Sets total number of results
     * @param total total number of results
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * Sets EFV facet tree
     * @param efvFacet tree of EFVs with {@link FacetUpDn} objects as payload
     */
    public void setEfvFacet(EfvTree<FacetUpDn> efvFacet)
    {
        this.efvFacet = efvFacet;
    }

    /**
     * Returns EFV facet tree
     * @return tree of EFVs with {@link ae3.service.structuredquery.FacetUpDn} objects as payload
     */
    public EfvTree<FacetUpDn> getEfvFacet()
    {
        return efvFacet;
    }

    /**
     * Returns map of gene facets
     * @return map of string to iterable list of {@link ae3.service.structuredquery.FacetCounter} objects
     */
    public Map<String,Iterable<FacetCounter>> getGeneFacets() {
        return geneFacets;
    }

    /**
     * Returns one gene facet by name
     * @param name facet name to get
     * @return iterable list of {@link ae3.service.structuredquery.FacetCounter} objects
     */
    public Iterable<FacetCounter> getGeneFacet(String name) {
        return geneFacets.get(name);
    }

    /**
     * Sets gene facet by name
     * @param name gene facet name
     * @param facet iterable list of {@link ae3.service.structuredquery.FacetCounter} objects
     */
    public void setGeneFacet(String name, Iterable<FacetCounter> facet) {
        this.geneFacets.put(name, facet);
    }

    /**
     * Returns query result condition
     * @return iterable list of conditions
     */
    public Iterable<Condition> getConditions() {
        return conditions;
    }

    /**
     * Sets list of query result conditions
     * @param conditions iterable list of conditions
     */
    public void setConditions(Iterable<Condition> conditions) {
        this.conditions = conditions;
    }

    /**
     * Returns set of EFs collapsed by heatmap trimming function and available for expansion
     * @return set of strings representing EF names
     */
    public Set<String> getExpandableEfs() {
        return expandableEfs;
    }

    /**
     * Sets set of EFs collapsed by heatmap trimming function and available for expansion
     * @param expandableEfs collection of strings
     */
    public void setExpandableEfs(Collection<String> expandableEfs) {
        this.expandableEfs = new HashSet<String>();
        this.expandableEfs.addAll(expandableEfs);
    }
}

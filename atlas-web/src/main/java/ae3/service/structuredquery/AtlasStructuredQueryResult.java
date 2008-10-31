package ae3.service.structuredquery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * @author pashky
 */
public class AtlasStructuredQueryResult {
    protected final Log log = LogFactory.getLog(getClass());

    public static class Condition  {
        private AtlasStructuredQuery.Condition condition;
        private EfvTree<Boolean> expansion;

        public Condition(AtlasStructuredQuery.Condition condition, EfvTree<Boolean> expansion) {
            this.condition = condition;
            this.expansion = expansion;
        }

        public AtlasStructuredQuery.Expression getExpression() {
            return condition.getExpression();
        }

        public String getFactor() {
            return condition.getFactor();
        }

        public Iterable<String> getFactorValues() {
            return condition.getFactorValues();
        }

        public EfvTree<Boolean> getExpansion() {
            return expansion;
        }
    }

    private EfvTree<Integer> queryEfvs;
    private Collection<StructuredResultRow> results;
    private Iterable<Condition> conditions;

    private long total;
    private long start;
    private long rows;

    private EfvTree<FacetUpDn> efvFacet;
    private Map<String,Iterable<FacetCounter>> geneFacets;

    public AtlasStructuredQueryResult(long start, long rows) {
        this.results = new ArrayList<StructuredResultRow>();
        this.geneFacets = new HashMap<String, Iterable<FacetCounter>>();
        this.start = start;
        this.rows = rows;
    }

    public void addResult(StructuredResultRow result) {
        results.add(result);
    }

    public int getSize() {
        return results.size();
    }

    public Iterable<StructuredResultRow> getResults() {
        return results;
    }

    public void setQueryEfvs(EfvTree<Integer> queryEfvs) {
        this.queryEfvs = queryEfvs;
    }

    public EfvTree<Integer> getQueryEfvs() {
        return queryEfvs;
    }

    public long getStart() {
        return start;
    }

    public long getTotal() {
        return total;
    }

    public long getRows() {
        return rows;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setEfvFacet(EfvTree<FacetUpDn> efvFacet)
    {
        this.efvFacet = efvFacet;
    }

    public EfvTree<FacetUpDn> getEfvFacet()
    {
        return efvFacet;
    }

    public Map<String,Iterable<FacetCounter>> getGeneFacets() {
        return geneFacets;
    }

    public Iterable<FacetCounter> getGeneFacet(String name) {
        return geneFacets.get(name);
    }

    public void setGeneFacet(String name, Iterable<FacetCounter> facet) {
        this.geneFacets.put(name, facet);
    }

    public Iterable<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(Iterable<Condition> conditions) {
        this.conditions = conditions;
    }

}

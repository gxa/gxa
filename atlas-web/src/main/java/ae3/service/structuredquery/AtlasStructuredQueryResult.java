package ae3.service.structuredquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ae3.service.ListResultRow;

import java.util.*;

/**
 * Atlas structured query result container class
 * @author pashky
 */
public class AtlasStructuredQueryResult {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private EfvTree<Integer> resultEfvs;
    private Collection<StructuredResultRow> results;
    private Collection<ListResultRow> listResults;
    private Iterable<ExpFactorResultCondition> conditions;
    private Set<String> expandableEfs;

    private long total;
    private long start;
    private long rowsPerPage;
    private int rowsPerGene;
    private int numListGenes;

    private EfvTree<FacetUpDn> efvFacet;
    private Map<String,Iterable<FacetCounter>> geneFacets;

    private boolean hasEFOExpansion = false;

    /**
     * Constructor
     * @param start starting position in paging
     * @param rowsPerPage number of rows in page
     */
    public AtlasStructuredQueryResult(long start, long rowsPerPage) {
        this.results = new ArrayList<StructuredResultRow>();
        this.listResults = new ArrayList<ListResultRow>();
        this.geneFacets = new HashMap<String, Iterable<FacetCounter>>();
        this.start = start;
        this.rowsPerPage = rowsPerPage;
        this.rowsPerGene = 10;
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
     * Returns sorted list of atlas list view results sorted by number of studies and p-value
     * @return
     */
    public Collection<ListResultRow> getListResults() {
    	Collections.sort((ArrayList<ListResultRow>)listResults,Collections.reverseOrder());
		return listResults;
	}

    /**
     * Adds listResult to list
     * @param listRow to add
     */
	public void addListResult(ListResultRow listRow) {
		this.listResults.add(listRow);
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
     * Returns results current page number
     * @return page number
     */
    public long getPage() {
        return getStart() / getRowsPerPage();
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
    public long getRowsPerPage() {
        return rowsPerPage;
    }

    /**
     * Returns number of rows (Factor values) allowed to show in list view per gene
     * @return
     */
    public int getRowsPerGene() {
		return rowsPerGene;
	}

	public void setRowsPerGene(int listRowsPerGene) {
		this.rowsPerGene = listRowsPerGene;
	}


	public int getNumListGenes() {
		return numListGenes;
	}

	public void setNumListGenes(int numListGenes) {
		this.numListGenes = numListGenes;
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
    public Iterable<ExpFactorResultCondition> getConditions() {
        return conditions;
    }

    /**
     * Sets list of query result conditions
     * @param conditions iterable list of conditions
     */
    public void setConditions(Iterable<ExpFactorResultCondition> conditions) {
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

    public boolean isHasEFOExpansion() {
        return hasEFOExpansion;
    }

    public void setHasEFOExpansion(boolean hasEFOExpansion) {
        this.hasEFOExpansion = hasEFOExpansion;
    }
}

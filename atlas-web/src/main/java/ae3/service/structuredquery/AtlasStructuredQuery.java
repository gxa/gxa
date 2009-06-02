package ae3.service.structuredquery;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;

/**
 * Atlas structured query container class for parsed parameters
 * @author pashky
 */
public class AtlasStructuredQuery {

    private Iterable<String> species;
    private Iterable<ExpFactorQueryCondition> conditions;
    private Iterable<GeneQueryCondition> geneConditions;
    private int start;
    private int rowsPerPage;
    private Set<String> expandColumns;
    private ViewType viewType;
    private boolean export;

    public AtlasStructuredQuery() {
        conditions = new ArrayList<ExpFactorQueryCondition>(0);
        geneConditions = new ArrayList<GeneQueryCondition>(0);
        start = 0;
        rowsPerPage = 100;
    }

    /**
     * sets lists of gene queries represented by each row added to the query
     * @param geneConditions
     */
    public void setGeneConditions(List<GeneQueryCondition> geneConditions){
    	this.geneConditions = geneConditions;
    }
    
    /**
     * Returns gene queries for the current query. Includes for each query (query, query operator and gene property)
     * @return geneQueries
     */
    public Iterable<GeneQueryCondition> getGeneConditions(){
    	return geneConditions;
    }
    
    /**
     * Returns list of species
     * @return list of species
     */
    public Iterable<String> getSpecies() {
        return species;
    }

    /**
     * Sets list of species
     * @param species list of species
     */
    public void setSpecies(Iterable<String> species) {
        this.species = species;
    }

    /**
     * Returns iterable of all conditions
     * @return iterable of all conditions
     * @see ExpFactorQueryCondition
     */
    public Iterable<ExpFactorQueryCondition> getConditions() {
        return conditions;
    }

    /**
     * Sets list of EFV conditions
     * @param conditions list of EFV conditions
     * @see QueryCondition
     */
    public void setConditions(Iterable<ExpFactorQueryCondition> conditions) {
        this.conditions = conditions;
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
    public int getRowsPerPage() {
        return rowsPerPage;
    }

    /**
     * Sets required number of rows in page
     * @param rowsPerPage number of rows in page
     */
    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
    }

    /**
     * Checks if query is "simple" query (just one condition)
     * @return true or false
     */
    public boolean isSimple() {
        Iterator<ExpFactorQueryCondition> efi = conditions.iterator();
        Iterator<GeneQueryCondition> gqi = geneConditions.iterator();
        Iterator<String> spi = species.iterator();
        return (!efi.hasNext() || ("".equals(efi.next().getFactor())) && !efi.hasNext()) &&
                (!gqi.hasNext() || (!gqi.next().isNegated() && !gqi.hasNext())) &&
                (!spi.hasNext() || (spi.next() != null && !spi.hasNext())); 
    }

    /**
     * Checks if there's not enough parameters entered to query for something
     * @return true or false
     */
    public boolean isNone() {
        return !conditions.iterator().hasNext() && !geneConditions.iterator().hasNext();
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
    
    /**
     * Sets requested view for the output results
     */
    public void setViewType(ViewType viewType){
    	this.viewType = viewType;
    }

    public ViewType getViewType() {
        return viewType;
    }

	public boolean isExport() {
		return export;
	}

	public void setExport(boolean export) {
		this.export = export;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();

        boolean hasValues = false;

		for (GeneQueryCondition c : geneConditions){
			if(!hasValues)
				sb.append(" and ");
            sb.append(c.getJointFactorValues());
            hasValues = true;
		}
        if(!hasValues)
            sb.append("(All genes) ");

        hasValues = false;
		for(ExpFactorQueryCondition c : conditions){
            if(!hasValues)
                sb.append(" and ");

			sb.append(c.getExpression().getDescription()).append(" in ");
			if(c.isAnything())
				sb.append("(all conditions)");
			else
				sb.append(c.getJointFactorValues()).append("\n");
		}
		return sb.toString();
	}
}

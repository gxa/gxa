/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package ae3.service.structuredquery;

import ae3.model.AtlasGene;
import ae3.model.ListResultRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.MappingIterator;

import java.util.*;

/**
 * Atlas structured query result container class
 * @author pashky
 */
public class AtlasStructuredQueryResult {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private EfvTree<ColumnInfo> resultEfvs;
    private EfoTree<ColumnInfo> resultEfos;
    private final List<StructuredResultRow> results = new ArrayList<StructuredResultRow>();
    private final Map<AtlasGene, List<ListResultRow>> listResults = new LinkedHashMap<AtlasGene, List<ListResultRow>>();
    private final List<ExpFactorResultCondition> conditions = new ArrayList<ExpFactorResultCondition>();
    private Set<String> expandableEfs;

    private long total;
    private long start;
    private long rowsPerPage;
    private int rowsPerGene;
    // Variable used to return error message to the user
    private String userErrorMsg = null;

    private EfvTree<FacetUpDn> efvFacet;
    private final Map<String,Iterable<FacetCounter>> geneFacets = new HashMap<String, Iterable<FacetCounter>>();

    /**
     * Constructor
     * @param start starting position in paging
     * @param rowsPerPage number of rows in page
     */
    public AtlasStructuredQueryResult(long start, long rowsPerPage, int expsPerGene) {
        this.start = start;
        this.rowsPerPage = rowsPerPage;
        this.rowsPerGene = expsPerGene;
    }

    /**
     * Adds result to list
     * @param result result to add
     */
    public void addResult(StructuredResultRow result) {
        results.add(result);
    }

    /**
     *
     * @param userErrorMsg error message to be presented back to the user
     */
    public void setUserErrorMsg(String userErrorMsg) {
        this.userErrorMsg = userErrorMsg;
    }

    /**
     *
     * @return error message to be presented back to the user
     */
    public String getUserErrorMsg() {
        return userErrorMsg;
    }

    /**
     * Returns number of results
     * @return number of results in result list
     */
    @RestOut(name="numberOfResultGenes")
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
     * Returns tree of resulting EFOs
     * @return result EFO terms
     */
    public EfoTree<ColumnInfo> getResultEfos() {
        return resultEfos;
    }

    /**
     * Sets result EFO terms
     * @param resultEfos efvtree of result EFO columns
     */
    public void setResultEfos(EfoTree<ColumnInfo> resultEfos) {
        this.resultEfos = resultEfos;
    }

    /**
     * Returns sorted list of atlas list view results sorted by number of studies and p-value
     * @return list of result rows
     */
    public List<ListResultRow> getListResults() {
        long start = System.currentTimeMillis();
        List<ListResultRow> allRows = new ArrayList<ListResultRow>();
        Map<String, List<ListResultRow>> efToListResultRows = new TreeMap<String, List<ListResultRow>>();
         for(List<ListResultRow> rows : listResults.values()) {
             for (ListResultRow row : rows) {
                 String ef = row.getEf();
                 if (!efToListResultRows.containsKey(ef)) {
                    efToListResultRows.put(ef, new ArrayList<ListResultRow>());
                 }
                 efToListResultRows.get(ef).add(row);
             }
         }

        for(List<ListResultRow> rows : efToListResultRows.values()) {
            Collections.sort(rows,Collections.reverseOrder());
            allRows.addAll(rows);
        }
        log.debug("Got list results in: " +(System.currentTimeMillis() - start) + " ms");
		return allRows;
	}


    /**
     * @param gene
     * @return Number of gene's list result rows in list view (each list view row corresponds to a
     *         single gene-ef-efv combination)
     */
    public int getNumberOfListResultsForGene(AtlasGene gene) {
        List<ListResultRow> listResultsPerGene = listResults.get(gene);
        if (listResultsPerGene != null) {
            return listResultsPerGene.size();
        }
        return 0;
    }


    /**
     * Adds listResult to list
     * @param listRow to add
     */
	public void addListResult(ListResultRow listRow) {
        List<ListResultRow> list = listResults.get(listRow.getGene());
        if(list == null)
            listResults.put(listRow.getGene(), list = new ArrayList<ListResultRow>());
		list.add(listRow);
	}

    /**
     * Result gene class aggregating several list results for one gene
     */
    public static class ListResultGene {
        private List<ListResultRow> rows;
        public ListResultGene(List<ListResultRow> rows) { this.rows = rows; }
        public AtlasGene getGene() { return rows.get(0).getGene(); }
        public List<ListResultRow> getExpressions() { return rows; }
    }

    @RestOut(name="genes")
    public Iterable<ListResultGene> getListResultsGenes() {
        return new Iterable<ListResultGene>() {
            public Iterator<ListResultGene> iterator() {
                return new MappingIterator<List<ListResultRow>, ListResultGene>(listResults.values().iterator()) {
                    public ListResultGene map(List<ListResultRow> listResultRows) {
                        return new ListResultGene(listResultRows);
                    }
                };
            }
        };
    }

	/**
     * Set results EFVs tree
     * @param resultEfvs result EFVs tree
     */
    public void setResultEfvs(EfvTree<ColumnInfo> resultEfvs) {
        this.resultEfvs = resultEfvs;
    }

    /**
     * Returns result EFVs tree
     * @return tree of result EFV columns
     */
    public EfvTree<ColumnInfo> getResultEfvs() {
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
    @RestOut(name="startingFrom")
    public long getStart() {
        return start;
    }

    /**
     * Returns total number of results
     * @return total number of results
     */
    @RestOut(name="totalResultGenes")
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

    /**
     * Sets number of rows per gene
     * @param listRowsPerGene maximum number of list rows per gene       
     */
	public void setRowsPerGene(int listRowsPerGene) {
		this.rowsPerGene = listRowsPerGene;
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
     * @return list of conditions
     */
    public Collection<ExpFactorResultCondition> getConditions() {
        return Collections.unmodifiableCollection(conditions);
    }

    /**
     * Sets list of query result conditions
     * @param conditions iterable list of conditions
     */
    public void setConditions(Collection<ExpFactorResultCondition> conditions) {
        this.conditions.clear();
        this.conditions.addAll(conditions);
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

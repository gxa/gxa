package  uk.ac.ebi.gxa.model;

/**
 * Parameters for paging-sorting: Start, Rows, SortOrder.
 * User: Andrey
 * Date: Oct 15, 2009
 * Time: 2:08:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageSortParams implements java.io.Serializable {

    public static final PageSortParams ALL = new PageSortParams(0, Integer.MAX_VALUE, "");

    protected int start = 0;

    public PageSortParams() {
    }

    public PageSortParams(int start, int rows, String sortOrder) {
        this.start = start;
        this.rows = rows;
        this.sortOrder = sortOrder;
    }

    public int getStart(){
        return start;
    }
    public PageSortParams setStart(int start){
        this.start = start;
        return this;
    }

    protected int rows = 100;
    
    public int getRows(){
        return rows;
    }
    public PageSortParams setRows(int rows){
        this.rows = rows;
        return this;
    }

    protected String sortOrder;
                             
    public PageSortParams orderBy(String sortOrder){  //rank_by_expression_algorithm_23
        this.sortOrder = sortOrder;
        return this;
    }
    public String getOrderBy(){
        return this.sortOrder;
    }

}
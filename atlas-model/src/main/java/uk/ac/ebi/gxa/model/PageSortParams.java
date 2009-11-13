package  uk.ac.ebi.gxa.model;

/**
 * Parameters for paging-sorting: Start, Rows, SortOrder.
 * User: Andrey
 * Date: Oct 15, 2009
 * Time: 2:08:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageSortParams implements java.io.Serializable {
    private int start = 0;
    
    public int getStart(){
        return start;
    }
    public PageSortParams setStart(int start){
        this.start = start;
        return this;
    }

    private int rows = 100;
    
    public int getRows(){
        return rows;
    }
    public PageSortParams setRows(int rows){
        this.rows = rows;
        return this;
    }

    public PageSortParams orderBy(String sortOrder){  //rank_by_expression_algorithm_23
        return this;
    }

}
package uk.ac.ebi.gxa.model;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Dec 3, 2009
 * Time: 1:56:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class PageSortParamsAll extends PageSortParams{
    public PageSortParamsAll(){
        this.start = 0;
        this.rows = Integer.MAX_VALUE;
    }
}

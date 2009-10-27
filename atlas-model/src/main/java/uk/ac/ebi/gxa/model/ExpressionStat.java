package  uk.ac.ebi.gxa.model;

/**
 * Defining data on gene expression.
 * User: Andrey
 * Date: Oct 22, 2009
 * Time: 11:12:59 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ExpressionStat {

    /* remainings of not-type-safe stats */

    
    

    /* ranking algorithm supposedly supplied by caller*/
    public Float getRank();

    public Integer getUpExperimentsCount();
    public Integer getDnExperimentsCount();
    public Double getUpPvalue();
    public Double getDnPvalue();

    public Iterable<ExpressionStat> drillDown();
}

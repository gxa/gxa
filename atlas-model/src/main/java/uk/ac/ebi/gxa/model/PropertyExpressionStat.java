package  uk.ac.ebi.gxa.model;

/**
 * ExpressionStat drilldown by property.
 * User: Andrey
 * Date: Oct 26, 2009
 * Time: 4:31:41 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PropertyExpressionStat<NextType extends ExpressionStat> extends ExpressionStat<NextType> {
    public Property getProperty();
}

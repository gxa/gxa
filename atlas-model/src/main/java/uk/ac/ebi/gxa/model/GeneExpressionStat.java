package  uk.ac.ebi.gxa.model;

/**
 * ExpressionStat drilldown by Gene.
 * User: Andrey
 * Date: Oct 26, 2009
 * Time: 4:29:43 PM
 * To change this template use File | Settings | File Templates.
 */
public interface GeneExpressionStat extends ExpressionStat {
  public String getGene();
}

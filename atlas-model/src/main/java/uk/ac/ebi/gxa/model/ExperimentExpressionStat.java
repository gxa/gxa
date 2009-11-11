package  uk.ac.ebi.gxa.model;

/**
 * ExpressionStat drilldown by Experiment.
 * User: Andrey
 * Date: Oct 26, 2009
 * Time: 4:32:33 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ExperimentExpressionStat<NextType extends ExpressionStat> extends ExpressionStat<NextType> {
   public String getExperiment();
}

package uk.ac.ebi.gxa.analytics.compute;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop
 * Date: Jun 19, 2009
 * Time: 6:16:22 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Compute {
    <T> T computeTask(ComputeTask<T> task);
}

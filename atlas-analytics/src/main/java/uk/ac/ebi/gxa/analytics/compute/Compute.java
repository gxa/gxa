package uk.ac.ebi.gxa.analytics.compute;

/**
 * Interface for performing computation of mathematical tasks.
 *
 * @author Misha Kapushesky
 * @date Jun 19, 2009
 */
public interface Compute {
    <T> T computeTask(ComputeTask<T> task);
}

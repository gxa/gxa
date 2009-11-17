package uk.ac.ebi.gxa.analytics.compute;

import org.kchine.r.server.RServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.R.AtlasRFactory;

/**
 * Provides access to R computational infrastructure via an AtlasRFactory. To use, pass a {@link ComputeTask} to the
 * method {@link #computeTask(ComputeTask)}, the return type is determined by the type parameter to {@code
 * ComputeTask}.
 * <p/>
 * For example:
 * <code><pre>
 * RNumeric i = computeService.computeTask(new ComputeTask<RNumeric> () {
 *   public compute(RServices R) throws RemoteException {
 *     return (RNumeric) R.getObject("1 + 3");
 *   }
 * );
 * </pre></code>
 *
 * @author Misha Kapushesky
 * @author Tony Burdett
 */
public class AtlasComputeService implements Compute {
    private AtlasRFactory atlasRFactory;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public AtlasRFactory getAtlasRFactory() {
        return atlasRFactory;
    }

    public void setAtlasRFactory(AtlasRFactory atlasRFactory) {
        this.atlasRFactory = atlasRFactory;
    }

    /**
     * Executes task on a borrowed worker. Returns type specified in generic type parameter T to the method.
     *
     * @param task task to evaluate, {@link ComputeTask}
     * @param <T>  type that the task returns on completion
     * @return T
     */
    public <T> T computeTask(ComputeTask<T> task) {
        RServices rService = null;
        try {
            log.debug("Acquiring RServices");
            rService = getAtlasRFactory().createRServices();

            log.debug("Computing on " + rService.getServantName());
            return task.compute(rService);
        }
        catch (Exception e) {
            log.error("Problem computing task!", e.getMessage());
            return null;
        }
        finally {
            if (null != rService) {
                try {
                    log.info("Recycling R service");
                    getAtlasRFactory().recycleRServices(rService);
                }
                catch (Exception e) {
                    log.error("Problem returning worker! {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Releases any resources that are retained by this AtlasComputeService
     */
    public void shutdown() {
        atlasRFactory.releaseResources();
    }
}

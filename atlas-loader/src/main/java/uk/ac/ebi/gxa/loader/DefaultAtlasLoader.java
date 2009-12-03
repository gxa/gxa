package uk.ac.ebi.gxa.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderEvent;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderService;
import uk.ac.ebi.gxa.loader.service.AtlasMAGETABLoader;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A default implementation of {@link uk.ac.ebi.gxa.loader.AtlasLoader} that loads experiments and array designs
 * referenced by URL.  It can be configured with a URL pointing to the root path of all experiments, but this is not
 * used - load operations should always be supplied with the full URL to the file to load.
 * <p/>
 * Internally, this class uses an {@link uk.ac.ebi.gxa.loader.service.AtlasMAGETABLoader} to perform all loading
 * operations by default: as such, all experiments and array designs should be supplied in MAGE-TAB format.
 *
 * @author Tony Burdett
 * @date 27-Nov-2009
 */
public class DefaultAtlasLoader implements AtlasLoader<URL, URL>, InitializingBean {
    private AtlasDAO atlasDAO;
    private URL repositoryLocation;
    private double missingDesignElementsCutoff = -1;

    private AtlasLoaderService<URL> atlasLoaderService;

    private ExecutorService service;
    private boolean running = false;

    // logging
    private final Logger log = LoggerFactory.getLogger(getClass());

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public URL getRepositoryLocation() {
        return repositoryLocation;
    }

    public void setRepositoryLocation(URL repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

    public double getMissingDesignElementsCutoff() {
        return missingDesignElementsCutoff;
    }

    public void setMissingDesignElementsCutoff(double missingDesignElementsCutoff) {
        this.missingDesignElementsCutoff = missingDesignElementsCutoff;
    }

    public void afterPropertiesSet() throws Exception {
        startup();
    }

    public void startup() throws AtlasLoaderException {
        if (!running) {
            // do some initialization...

            // create the service
            atlasLoaderService = new AtlasMAGETABLoader(atlasDAO);
            // if we have set the cutoff for missing design elements, set on the service
            if (missingDesignElementsCutoff != -1) {
                atlasLoaderService.setMissingDesignElementsCutoff(missingDesignElementsCutoff);
            }

            // finally, create an executor service for processing calls to build the index
            service = Executors.newCachedThreadPool();

            running = true;
        }
        else {
            log.warn("Ignoring attempt to startup() a " + getClass().getSimpleName() + " that is already running");
        }
    }

    public void shutdown() throws AtlasLoaderException {
        if (running) {
            log.debug("Shutting down " + getClass().getSimpleName() + "...");
            service.shutdown();
            try {
                log.debug("Waiting for termination of running jobs");
                service.awaitTermination(60, TimeUnit.SECONDS);

                if (!service.isTerminated()) {
                    // try and halt immediately
                    List<Runnable> tasks = service.shutdownNow();
                    service.awaitTermination(15, TimeUnit.SECONDS);
                    // if it's STILL not terminated...
                    if (!service.isTerminated()) {
                        StringBuffer sb = new StringBuffer();
                        sb.append("Unable to cleanly shutdown Atlas loader service.\n");
                        if (tasks.size() > 0) {
                            sb.append("The following tasks are still active or suspended:\n");
                            for (Runnable task : tasks) {
                                sb.append("\t").append(task.toString()).append("\n");
                            }
                        }
                        sb.append("There are running or suspended Atlas loading tasks. " +
                                "If execution is complete, or has failed to exit " +
                                "cleanly following an error, you should terminate this " +
                                "application");
                        log.error(sb.toString());
                        throw new AtlasLoaderException(sb.toString());
                    }
                    else {
                        // it worked second time round
                        log.debug("Shutdown complete");
                    }
                }
                else {
                    log.debug("Shutdown complete");
                }
            }
            catch (InterruptedException e) {
                log.error("The application was interrupted whilst waiting to " +
                        "be shutdown.  There may be tasks still running or suspended.");
                throw new AtlasLoaderException(e);
            }
            finally {
                running = false;
            }
        }
        else {
            log.warn(
                    "Ignoring attempt to shutdown() a " + getClass().getSimpleName() +
                            " that is not running");
        }
    }

    public void loadExperiment(URL experimentResource) {
        loadExperiment(experimentResource, null);
    }

    public void loadExperiment(final URL experimentResource, final AtlasLoaderListener listener) {
        final long startTime = System.currentTimeMillis();
        final List<Future<Boolean>> buildingTasks =
                new ArrayList<Future<Boolean>>();

        buildingTasks.add(service.submit(new Callable<Boolean>() {
            public Boolean call() throws AtlasLoaderException {
                try {
                    log.info("Starting load operation on " + experimentResource.toString());

                    atlasLoaderService.load(experimentResource);

                    log.debug("Finished load operation on " + experimentResource.toString());

                    return true;
                }
                catch (Exception e) {
                    log.error("Caught unchecked exception: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        }));

        // this tracks completion, if a listener was supplied
        if (listener != null) {
            new Thread(new Runnable() {
                public void run() {
                    boolean success = true;
                    List<Throwable> observedErrors = new ArrayList<Throwable>();

                    // wait for expt and gene indexes to build
                    for (Future<Boolean> buildingTask : buildingTasks) {
                        try {
                            success = success && buildingTask.get();
                        }
                        catch (Exception e) {
                            observedErrors.add(e);
                            success = false;
                        }
                    }

                    // now we've finished - get the end time, calculate runtime and fire the event
                    long endTime = System.currentTimeMillis();
                    long runTime = (endTime - startTime) / 1000;

                    // create our completion event
                    if (success) {
                        listener.loadSuccess(new AtlasLoaderEvent(
                                runTime, TimeUnit.SECONDS));
                    }
                    else {
                        listener.loadError(new AtlasLoaderEvent(
                                runTime, TimeUnit.SECONDS, observedErrors));
                    }
                }
            }).start();
        }
    }

    public void loadArrayDesign(URL arrayDesignResource) {
        loadArrayDesign(arrayDesignResource, null);
    }

    public void loadArrayDesign(final URL arrayDesignResource, final AtlasLoaderListener listener) {
        throw new UnsupportedOperationException("Array Design loading not yet supported");
    }
}

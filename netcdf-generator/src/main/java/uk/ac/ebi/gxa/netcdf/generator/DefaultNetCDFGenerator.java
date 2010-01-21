package uk.ac.ebi.gxa.netcdf.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGenerationEvent;
import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGeneratorListener;
import uk.ac.ebi.gxa.netcdf.generator.service.ExperimentNetCDFGeneratorService;
import uk.ac.ebi.gxa.netcdf.generator.service.NetCDFGeneratorService;
import uk.ac.ebi.gxa.utils.Deque;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A default implementation of {@link NetCDFGenerator} that builds a NetCDF repository at a given {@link File} on the
 * local filesystem.
 *
 * @author Tony Burdett
 * @date 17-Sep-2009
 */
public class DefaultNetCDFGenerator implements NetCDFGenerator<File>, InitializingBean {
    private AtlasDAO atlasDAO;
    private File repositoryLocation;

    private NetCDFGeneratorService netCDFService;

    private ExecutorService service;
    private boolean running = false;

    // logging
    private final Logger log =
            LoggerFactory.getLogger(DefaultNetCDFGenerator.class);

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setRepositoryLocation(File repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

    public File getRepositoryLocation() {
        return repositoryLocation;
    }

    public void afterPropertiesSet() throws Exception {
        // simply delegates to startup(), this allows automated spring startup
        startup();
    }

    public void startup() throws NetCDFGeneratorException {
        if (!running) {
            // do some initialization...

            // check the repository location exists, or else create it
            if (!repositoryLocation.exists()) {
                if (!repositoryLocation.mkdirs()) {
                    log.error("Couldn't create " + repositoryLocation.getAbsolutePath());
                    throw new NetCDFGeneratorException("Unable to create NetCDF " +
                            "repository at " + repositoryLocation.getAbsolutePath());
                }
            }

            // create the service
            netCDFService = new ExperimentNetCDFGeneratorService(atlasDAO, repositoryLocation);

            // finally, create an executor service for processing calls to build the index
            service = Executors.newCachedThreadPool();

            running = true;
        }
        else {
            log.warn("Ignoring attempt to startup() a " + getClass().getSimpleName() + " that is already running");
        }
    }

    public void shutdown() throws NetCDFGeneratorException {
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
                        sb.append("Unable to cleanly shutdown NetCDF generating service.\n");
                        if (tasks.size() > 0) {
                            sb.append("The following tasks are still active or suspended:\n");
                            for (Runnable task : tasks) {
                                sb.append("\t").append(task.toString()).append("\n");
                            }
                        }
                        sb.append("There are running or suspended NetCDF generating tasks. " +
                                "If execution is complete, or has failed to exit " +
                                "cleanly following an error, you should terminate this " +
                                "application");
                        log.error(sb.toString());
                        throw new NetCDFGeneratorException(sb.toString());
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
                throw new NetCDFGeneratorException(e);
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

    public void generateNetCDFs() {
        generateNetCDFs(null);
    }

    public void generateNetCDFs(final NetCDFGeneratorListener listener) {
        generateNetCDFsForExperiment(null, listener);
    }

    public void generateNetCDFsForExperiment(String experimentAccession) {
        generateNetCDFsForExperiment(experimentAccession, null);
    }

    public void generateNetCDFsForExperiment(
            final String experimentAccession,
            final NetCDFGeneratorListener listener) {
        final long startTime = System.currentTimeMillis();
        final Deque<Future<Boolean>> buildingTasks = new Deque<Future<Boolean>>(5);

        buildingTasks.offerLast(service.submit(new Callable<Boolean>() {
            public Boolean call() throws NetCDFGeneratorException {
                try {
                    log.info("Starting NetCDF generations");

                    if (experimentAccession == null) {
                        netCDFService.generateNetCDFs();
                    }
                    else {
                        netCDFService.generateNetCDFsForExperiment(experimentAccession);
                    }

                    log.debug("Finished NetCDF generations");

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

                    // wait for task to complete
                    while (true) {
                        try {
                            Future<Boolean> task = buildingTasks.poll();
                            if (task == null) {
                                break;
                            }
                            success = success && task.get();
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
                        listener.buildSuccess(new NetCDFGenerationEvent(
                                runTime, TimeUnit.SECONDS));
                    }
                    else {
                        listener.buildError(new NetCDFGenerationEvent(
                                runTime, TimeUnit.SECONDS, observedErrors));
                    }
                }
            }).start();
        }
        else {
            // just slam through all tasks, ignoring the results
            while(true) {
                if (buildingTasks.poll() == null) {
                    break;
                }
            }
        }
    }
}

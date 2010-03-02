/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.analytics.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGenerationEvent;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.analytics.generator.service.AnalyticsGeneratorService;
import uk.ac.ebi.gxa.analytics.generator.service.ExperimentAnalyticsGeneratorService;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.dao.AtlasDAO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A default implementation of {@link AnalyticsGenerator} that creates Atlas analytics in the database.
 *
 * @author Misha Kapushesky
 * @date 09-Nov-2009
 */
public class DefaultAnalyticsGenerator implements AnalyticsGenerator, InitializingBean {
    private AtlasDAO atlasDAO;
    private File repositoryLocation;
    private AtlasComputeService atlasComputeService;

    private AnalyticsGeneratorService analyticsService;

    private ExecutorService service;
    private boolean running = false;

    // logging
    private final Logger log =
            LoggerFactory.getLogger(DefaultAnalyticsGenerator.class);

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public File getRepositoryLocation() {
        return repositoryLocation;
    }

    public void setRepositoryLocation(File repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

    public AtlasComputeService getAtlasComputeService() {
        return atlasComputeService;
    }

    public void setAtlasComputeService(AtlasComputeService atlasComputeService) {
        this.atlasComputeService = atlasComputeService;
    }

    public void afterPropertiesSet() throws Exception {
        // simply delegates to startup(), this allows automated spring startup
        startup();
    }

    public void startup() throws AnalyticsGeneratorException {
        if (!running) {
            // do some initialization...

            // check the repository location exists, or else create it
            if (!repositoryLocation.exists()) {
                if (!repositoryLocation.mkdirs()) {
                    log.error("Couldn't create " + repositoryLocation.getAbsolutePath());
                    throw new AnalyticsGeneratorException("Unable to create NetCDF " +
                            "repository at " + repositoryLocation.getAbsolutePath());
                }
            }

            // create the service
            analyticsService = new ExperimentAnalyticsGeneratorService(atlasDAO, repositoryLocation, atlasComputeService);

            // finally, create an executor service for processing calls to build the index
            service = Executors.newCachedThreadPool();

            running = true;
        }
        else {
            log.warn("Ignoring attempt to startup() a " + getClass().getSimpleName() + " that is already running");
        }
    }

    public void shutdown() throws AnalyticsGeneratorException {
        if (running) {
            log.debug("Shutting down " + getClass().getSimpleName() + "...");

            // shutdown the compute service
            getAtlasComputeService().shutdown();

            // shutdown this service
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
                        throw new AnalyticsGeneratorException(sb.toString());
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
                throw new AnalyticsGeneratorException(e);
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

    public void generateAnalytics() {
        generateAnalytics(null);
    }

    public void generateAnalytics(final AnalyticsGeneratorListener listener) {
        generateAnalyticsForExperiment(null, listener);
    }

    public void generateAnalyticsForExperiment(String experimentAccession) {
        generateAnalyticsForExperiment(experimentAccession, null);
    }

    public void generateAnalyticsForExperiment(
            final String experimentAccession,
            final AnalyticsGeneratorListener listener) {
        final long startTime = System.currentTimeMillis();
        final List<Future<Boolean>> buildingTasks =
                new ArrayList<Future<Boolean>>();

        buildingTasks.add(service.submit(new Callable<Boolean>() {
            public Boolean call() throws AnalyticsGeneratorException {
                try {
                    if(listener != null)
                        listener.buildProgress("Processing...");

                    if (experimentAccession == null) {
                        log.info("Starting analytics generations for all experiments");
                        analyticsService.generateAnalytics();
                        log.info("Finished analytics generations for all experiments");
                    }
                    else {
                        analyticsService.generateAnalyticsForExperiment(experimentAccession);
                    }

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
                            success = buildingTask.get() && success;
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
                        listener.buildSuccess(new AnalyticsGenerationEvent(
                                runTime, TimeUnit.SECONDS));
                    }
                    else {
                        listener.buildError(new AnalyticsGenerationEvent(
                                runTime, TimeUnit.SECONDS, observedErrors));
                    }
                }
            }).start();
        }
    }
}

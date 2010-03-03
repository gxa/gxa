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

package uk.ac.ebi.gxa.loader;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderEvent;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;
import uk.ac.ebi.gxa.loader.service.AtlasArrayDesignLoader;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderService;
import uk.ac.ebi.gxa.loader.service.AtlasMAGETABLoader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
public class DefaultAtlasLoader implements AtlasLoader<URL>, InitializingBean {
    private AtlasDAO atlasDAO;
    private double missingDesignElementsCutoff = -1;
    private boolean allowReloading = false;
    private List<String> geneIdentifierPriority = new ArrayList<String>();

    private AtlasMAGETABLoader experimentLoaderService;
    private AtlasArrayDesignLoader arrayLoaderService;

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

    public double getMissingDesignElementsCutoff() {
        return missingDesignElementsCutoff;
    }

    public void setMissingDesignElementsCutoff(double missingDesignElementsCutoff) {
        this.missingDesignElementsCutoff = missingDesignElementsCutoff;
    }

    public boolean getAllowReloading() {
        return allowReloading;
    }

    public void setAllowReloading(boolean allowReloading) {
        this.allowReloading = allowReloading;
    }

    public List<String> getGeneIdentifierPriority() {
        return geneIdentifierPriority;
    }

    public void setGeneIdentifierPriority(List<String> geneIdentifierPriority) {
        this.geneIdentifierPriority = geneIdentifierPriority;
    }

    public void afterPropertiesSet() throws Exception {
        startup();
    }

    public void startup() throws AtlasLoaderException {
        if (!running) {
            // do some initialization...

            // create the experiment loading service
            experimentLoaderService = new AtlasMAGETABLoader(getAtlasDAO());
            experimentLoaderService.setAllowReloading(getAllowReloading());
            // if we have set the cutoff for missing design elements, set on the service
            if (missingDesignElementsCutoff != -1) {
                experimentLoaderService.setMissingDesignElementsCutoff(getMissingDesignElementsCutoff());
            }

            // create the experiment loading service
            arrayLoaderService = new AtlasArrayDesignLoader(getAtlasDAO());
            arrayLoaderService.setAllowReloading(getAllowReloading());
            arrayLoaderService.setGeneIdentifierPriority(getGeneIdentifierPriority());

            // finally, create an executor service for processing calls to load
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

    public void loadArrayDesign(URL arrayDesignResource) {
        loadArrayDesign(arrayDesignResource, null);
    }

    public void loadExperiment(final URL experimentResource, final AtlasLoaderListener listener) {
        loadByService(experimentLoaderService, experimentResource, listener);
    }

    public void loadArrayDesign(final URL arrayDesignResource, final AtlasLoaderListener listener) {
        loadByService(arrayLoaderService, arrayDesignResource, listener);
    }

    private void loadByService(final AtlasLoaderService<URL> loaderService, final URL experimentResource, final AtlasLoaderListener listener) {
        final long startTime = System.currentTimeMillis();
        service.submit(new Runnable() {
            public void run() {
                final List<String> accessions = new ArrayList<String>();
                final List<Throwable> errors = new ArrayList<Throwable>();
                try {
                    log.info("Starting load operation on " + experimentResource.toString());

                    boolean success = loaderService.load(experimentResource,
                            listener != null ? new AtlasLoaderService.Listener() {
                                public void setAccession(String accession) {
                                    accessions.add(accession);
                                }

                                public void setProgress(String progress) {
                                    listener.loadProgress(progress);
                                }
                            } : null);

                    log.info("Finished load operation on " + experimentResource.toString());
                    if(!success)
                        errors.add(new RuntimeException("Errors while storing " + StringUtils.join(accessions, ' ') + " to database"));
                }
                catch (Exception e) {
                    log.error("Loading error", e);
                    errors.add(e);
                }
                if(listener != null) {
                    long endTime = System.currentTimeMillis();
                    long runTime = (endTime - startTime) / 1000;
                    if(errors.isEmpty())
                        listener.loadSuccess(AtlasLoaderEvent.success(runTime, TimeUnit.SECONDS, accessions));
                    else
                        listener.loadError(AtlasLoaderEvent.error(runTime, TimeUnit.SECONDS, errors));
                }
            }
        });
    }

}

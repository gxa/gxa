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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.bioentity.ArrayDesignMappingLoader;
import uk.ac.ebi.gxa.loader.bioentity.AtlasBioentityAnnotationLoader;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderEvent;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;
import uk.ac.ebi.gxa.loader.service.*;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
 */
public class DefaultAtlasLoader implements AtlasLoader, InitializingBean {


    private ExecutorService service;
    private boolean running = false;

    // logging
    private final Logger log = LoggerFactory.getLogger(getClass());

    private AtlasMAGETABLoader magetabLoader;
    private AtlasArrayDesignLoader arrayDesignLoader;
    private AtlasExperimentUnloaderService experimentUnloaderService;
    private AtlasNetCDFUpdaterService netCDFUpdaterService;
    private AtlasVirtualArrayDesignLoader virtualArrayDesignLoader;
    private AtlasBioentityAnnotationLoader bioentityAnnotationLoader;
    private ArrayDesignMappingLoader designMappingLoader;
    private AtlasDataReleaseService dataReleaseService;

    public void afterPropertiesSet() throws Exception {
        startup();
    }

    public void startup() throws AtlasLoaderException {
        if (!running) {
            // finally, create an executor service for processing calls to load
            service = Executors.newCachedThreadPool();

            running = true;
        } else {
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
                    } else {
                        // it worked second time round
                        log.debug("Shutdown complete");
                    }
                } else {
                    log.debug("Shutdown complete");
                }
            } catch (InterruptedException e) {
                log.error("The application was interrupted whilst waiting to " +
                        "be shutdown.  There may be tasks still running or suspended.");
                throw new AtlasLoaderException(e);
            } finally {
                running = false;
            }
        } else {
            log.warn(
                    "Ignoring attempt to shutdown() a " + getClass().getSimpleName() +
                            " that is not running");
        }
    }

    private interface ServiceExecutionContext extends AtlasLoaderServiceListener, AtlasLoaderCommandVisitor {
    }

    public void doCommand(final AtlasLoaderCommand command, final AtlasLoaderListener listener) {
        service.submit(new Runnable() {
            public void run() {
                final List<String> accessions = new ArrayList<String>();
                final List<Throwable> errors = new ArrayList<Throwable>();
                final boolean[] recomputeAnalytics = new boolean[]{true};
                try {
                    log.info("Starting loader operation: " + command.toString());
                    command.visit(new ServiceExecutionContext() {
                        public void setAccession(String accession) {
                            accessions.add(accession);
                        }

                        public void setProgress(String progress) {
                            if (listener != null)
                                listener.loadProgress(progress);
                        }

                        public void setWarning(String warning) {
                            log.warn(warning);
                            if (listener != null)
                                listener.loadWarning(warning);
                        }

                        public void setRecomputeAnalytics(boolean recompute) {
                            recomputeAnalytics[0] = recompute;
                        }

                        public void process(LoadExperimentCommand cmd) throws AtlasLoaderException {
                            magetabLoader.process(cmd, this);
                        }

                        public void process(LoadArrayDesignCommand cmd) throws AtlasLoaderException {
                            arrayDesignLoader.process(cmd, this);
                        }

                        public void process(UnloadExperimentCommand cmd) throws AtlasLoaderException {
                           experimentUnloaderService.process(cmd, this);
                        }

                        public void process(UpdateNetCDFForExperimentCommand cmd) throws AtlasLoaderException {
                            netCDFUpdaterService.process(cmd, this);
                        }

                        public void process(LoadVirtualArrayDesignCommand cmd) throws AtlasLoaderException {
                            virtualArrayDesignLoader.process(cmd, this);
                        }

                        public void process(LoadBioentityCommand cmd) throws AtlasLoaderException {
                            bioentityAnnotationLoader.process(cmd, this);
                        }

                        public void process(LoadArrayDesignMappingCommand cmd) throws AtlasLoaderException {
                            designMappingLoader.process(cmd);
                        }

                        public void process(DataReleaseCommand cmd) throws AtlasLoaderException {
                            dataReleaseService.process(cmd);
                        }
                    });

                    log.info("Finished load operation: " + command.toString());
                } catch (Exception e) {
                    log.error("Loading error", e);
                    errors.add(e);
                }
                if (listener != null) {
                    if (errors.isEmpty())
                        listener.loadSuccess(AtlasLoaderEvent.success(accessions, recomputeAnalytics[0]));
                    else
                        listener.loadError(AtlasLoaderEvent.error(errors));
                }
            }
        });
    }

    public void setMagetabLoader(AtlasMAGETABLoader magetabLoader) {
        this.magetabLoader = magetabLoader;
    }

    public void setArrayDesignLoader(AtlasArrayDesignLoader arrayDesignLoader) {
        this.arrayDesignLoader = arrayDesignLoader;
    }

    public void setExperimentUnloaderService(AtlasExperimentUnloaderService experimentUnloaderService) {
        this.experimentUnloaderService = experimentUnloaderService;
    }

    public void setNetCDFUpdaterService(AtlasNetCDFUpdaterService netCDFUpdaterService) {
        this.netCDFUpdaterService = netCDFUpdaterService;
    }

    public void setVirtualArrayDesignLoader(AtlasVirtualArrayDesignLoader virtualArrayDesignLoader) {
        this.virtualArrayDesignLoader = virtualArrayDesignLoader;
    }

    public void setBioentityAnnotationLoader(AtlasBioentityAnnotationLoader bioentityAnnotationLoader) {
        this.bioentityAnnotationLoader = bioentityAnnotationLoader;
    }

    public void setDesignMappingLoader(ArrayDesignMappingLoader designMappingLoader) {
        this.designMappingLoader = designMappingLoader;
    }

    public void setDataReleaseService(AtlasDataReleaseService dataReleaseService) {
        this.dataReleaseService = dataReleaseService;
    }
}

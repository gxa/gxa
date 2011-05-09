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
import uk.ac.ebi.gxa.loader.bioentity.ArrayDesignMappingLoader;
import uk.ac.ebi.gxa.loader.bioentity.AtlasBioentityAnnotationLoader;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderEvent;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;
import uk.ac.ebi.gxa.loader.service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

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
public class DefaultAtlasLoader implements AtlasLoader {
    // logging
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ExecutorService executor;

    private AtlasMAGETABLoader magetabLoader;
    private AtlasExperimentUnloaderService experimentUnloaderService;
    private AtlasNetCDFUpdaterService netCDFUpdaterService;
    private AtlasBioentityAnnotationLoader bioentityAnnotationLoader;
    private ArrayDesignMappingLoader designMappingLoader;
    private AtlasDataReleaseService dataReleaseService;

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    private interface ServiceExecutionContext extends AtlasLoaderServiceListener, AtlasLoaderCommandVisitor {
    }

    public void doCommand(final AtlasLoaderCommand command, final AtlasLoaderListener listener) {
        executor.submit(new Runnable() {
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

                        public void process(UnloadExperimentCommand cmd) throws AtlasLoaderException {
                            experimentUnloaderService.process(cmd, this);
                        }

                        public void process(UpdateNetCDFForExperimentCommand cmd) throws AtlasLoaderException {
                            netCDFUpdaterService.process(cmd, this);
                        }

                        public void process(LoadBioentityCommand cmd) throws AtlasLoaderException {
                            bioentityAnnotationLoader.process(cmd, this);
                        }

                        public void process(LoadArrayDesignMappingCommand cmd) throws AtlasLoaderException {
                            designMappingLoader.process(cmd);
                        }

                        public void process(DataReleaseCommand cmd) {
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

    public void setExperimentUnloaderService(AtlasExperimentUnloaderService experimentUnloaderService) {
        this.experimentUnloaderService = experimentUnloaderService;
    }

    public void setNetCDFUpdaterService(AtlasNetCDFUpdaterService netCDFUpdaterService) {
        this.netCDFUpdaterService = netCDFUpdaterService;
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

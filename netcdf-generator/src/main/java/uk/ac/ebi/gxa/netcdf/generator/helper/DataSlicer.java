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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.netcdf.generator.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.Gene;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * A device for slicing the data in a single experiment into bits that are suitable for storing in NetCDFs.  Generally,
 * this means a single slice of data per array design, and each slice indexes every sample by the assay it is associated
 * with.  Only the assays appropriate for the paired array design are present.  This class basically takes an AtlasDAO
 * it can use to fetch any additional data, and performs the slicing on a supplied experiment.
 *
 * @author Tony Burdett
 * @date 30-Sep-2009
 */
public class DataSlicer {
    private AtlasDAO atlasDAO;

    // logger
    private Logger log = LoggerFactory.getLogger(getClass());

    public DataSlicer(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public Set<DataSlice> sliceExperiment(final Experiment experiment) throws DataSlicingException {
        // create a service to handle slicing tasks in parallel
        final ExecutorService service = Executors.newCachedThreadPool();
        log.debug("Started service " + service);


        // fetch array designs and iterate
        ExperimentSlicer exptSlicer = new ExperimentSlicer(service, experiment);
        exptSlicer.setAtlasDAO(getAtlasDAO());
        // submit this task
        Future<Set<DataSlice>> exptFetching = service.submit(exptSlicer);

        // wait for dataslicing to finish
        try {
            log.debug("Waiting for experiment slicing task to complete " +
                    "(fetch arrays and populate the dataslice set)");
            Set<DataSlice> results = exptFetching.get();
            log.debug("Experiment slicing task completed");

            log.debug("Returning the sliced data");
            return results;
        }
        catch (InterruptedException e) {
            throw new DataSlicingException(
                    "A thread handling data slicing was interrupted", e);
        }
        catch (ExecutionException e) {
            if (e.getCause() != null) {
                throw new DataSlicingException("A thread handling data slicing failed.  Caused by: " +
                        (e.getMessage() == null || e.getMessage().equals("")
                                ? e.getCause().getClass().getSimpleName()
                                : e.getMessage()),
                                               e.getCause());
            }
            else {
                throw new DataSlicingException("A thread handling data slicing failed", e);
            }
        }
        finally {
            // shutdown the service
            log.debug("Shutting down service " + service.toString());

            try {
                service.shutdown();
                service.awaitTermination(60, TimeUnit.SECONDS);
                if (!service.isTerminated()) {
                    //noinspection ThrowFromFinallyBlock
                    throw new DataSlicingException("Failed to terminate service for " + getClass().getSimpleName() +
                            " cleanly - suspended tasks were found");
                }
                else {
                    log.debug("Service " + service.toString() + " exited cleanly");
                }
            }
            catch (InterruptedException e) {
                //noinspection ThrowFromFinallyBlock
                throw new DataSlicingException("Failed to terminate service for " + getClass().getSimpleName() +
                        " cleanly - suspended tasks were found");
            }
        }
    }
}

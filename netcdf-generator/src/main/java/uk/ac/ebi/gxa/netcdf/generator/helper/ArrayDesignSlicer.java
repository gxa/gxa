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

package uk.ac.ebi.gxa.netcdf.generator.helper;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Oct-2009
 */
public class ArrayDesignSlicer extends CallableSlicer<DataSlice> {
    // required initial resources
    private final Experiment experiment;
    private final ArrayDesign arrayDesign;

    public ArrayDesignSlicer(ExecutorService service, Experiment experiment, ArrayDesign arrayDesign) {
        super(service);
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;
    }

    public DataSlice call() throws Exception {
        // the set of level three fetch tasks - per array design
        Set<Future> dataFetching = new HashSet<Future>();

        // create a new data slice, for this experiment and arrayDesign
        final DataSlice dataSlice = new DataSlice(experiment, arrayDesign);

        // run nested fetch tasks
        synchronized (dataFetching) {
            AssaySlicer assaySlicer =
                    new AssaySlicer(getService(), experiment, arrayDesign, dataSlice);
            assaySlicer.setAtlasDAO(getAtlasDAO());

            dataFetching.add(getService().submit(assaySlicer));
        }

        synchronized (dataFetching) {
            ExpressionValueSlicer evSlicer =
                    new ExpressionValueSlicer(getService(), experiment, arrayDesign, dataSlice);
            evSlicer.setAtlasDAO(getAtlasDAO());

            dataFetching.add(getService().submit(evSlicer));
        }

        // block until all data fetching tasks are complete
        synchronized (dataFetching) {
            getLog().debug("Waiting for all data slicing tasks to complete (modify each dataslice with required data)");
            for (Future task : dataFetching) {
                try {
                    task.get();
                }
                catch (InterruptedException e) {
                    throw new DataSlicingException("A thread handling data slicing was interrupted", e);
                }
                catch (ExecutionException e) {
                    getLog().error("A thread handling data slicing failed", e);
                    if (e.getCause() != null) {
                        throw new DataSlicingException("A thread handling data slicing failed.  Caused by: " +
                                (e.getMessage() == null ||
                                        e.getMessage().equals("")
                                        ? e.getCause().getClass().getSimpleName()
                                        : e.getMessage()),
                                e.getCause());
                    } else {
                        throw new DataSlicingException("A thread handling data slicing failed", e);
                    }
                }
            }
            getLog().debug("Data slicing tasks to populate " + dataSlice + " completed");
        }

        // now evaluate property mappings
        getLog().debug("Evaluating property/value/assay indices for " + dataSlice.toString());
        dataSlice.evaluatePropertyMappings();

        // save this dataslice
        getLog().debug("Compiled dataslice... " + dataSlice.toString());
        return dataSlice;
    }
}

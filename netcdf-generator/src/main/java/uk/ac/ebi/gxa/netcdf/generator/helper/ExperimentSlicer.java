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

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * A {@link CallableSlicer}
 * implementation that slices experiment level data.  This implementation must
 * be appropriately configured with an executor service, and experiment that
 * will be sliced
 *
 * @author Tony Burdett
 * @date 28-Oct-2009
 */
public class ExperimentSlicer extends CallableSlicer<Set<DataSlice>> {
    // required initial resources
    private final Experiment experiment;

    public ExperimentSlicer(ExecutorService service, Experiment experiment) {
        super(service);
        this.experiment = experiment;
    }

    public Set<DataSlice> call() throws Exception {
        getLog().debug("Fetching array design data for " + experiment.getAccession());
        List<ArrayDesign> arrays = getAtlasDAO().getArrayDesignByExperimentAccession(experiment.getAccession());

        // the set of array fetching tasks - one per array design
        Set<Future<DataSlice>> arrayFetching = new HashSet<Future<DataSlice>>();

        for (final ArrayDesign arrayDesign : arrays) {
            ArrayDesignSlicer arraySlicer =
                    new ArrayDesignSlicer(getService(), experiment, arrayDesign);
            arraySlicer.setAtlasDAO(getAtlasDAO());

            arrayFetching.add(getService().submit(arraySlicer));
        }

        // wait for array fetching tasks to complete
        Set<DataSlice> dataSlices = new HashSet<DataSlice>();
        synchronized (arrayFetching) {
            getLog().debug("Waiting for all array slicing tasks to complete (create and populate each dataslice " +
                    "with experiment and array data)");
            for (Future<DataSlice> task : arrayFetching) {
                dataSlices.add(task.get());
                getLog().debug("Array slicing task for " + task.get() + " complete");
            }
        }

        // and return
        getLog().debug("Compiled the set of all dataslices for " + experiment.getAccession());
        return dataSlices;
    }
}

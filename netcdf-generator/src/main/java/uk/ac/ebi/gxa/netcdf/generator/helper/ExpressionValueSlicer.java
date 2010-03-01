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

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Oct-2009
 */
public class ExpressionValueSlicer extends CallableSlicer<Void> {
    // required initial resources
    private final Experiment experiment;
    private final ArrayDesign arrayDesign;
    private final DataSlice dataSlice;

    public ExpressionValueSlicer(ExecutorService service,
                                 Experiment experiment,
                                 ArrayDesign arrayDesign,
                                 DataSlice dataSlice) {
        super(service);
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;
        this.dataSlice = dataSlice;
    }

    public Void call() throws Exception {
        // fetch expression values for this array
        getLog().debug(
                "Fetching expression values for " + experiment.getAccession() + " and " + arrayDesign.getAccession());
        Map<Integer, Map<Integer, Float>> expressionValues = getAtlasDAO().getExpressionValuesByExperimentAndArray(
                experiment.getExperimentID(), arrayDesign.getArrayDesignID());
        // and store
        dataSlice.storeExpressionValues(expressionValues);

        getLog().debug("Expression Value data for " + arrayDesign.getAccession() + " stored");
        return null;
    }
}

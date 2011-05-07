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

package ae3.model;

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Expression;

/**
 * @author pashky
 */
public class ListResultRowExperiment {
    private Expression updn;
    private float pvalue;
    private Experiment experiment;

    /**
     * Constructor
     *
     * @param experiment
     * @param pvalue     p-value
     * @param updn       up or down
     */
    public ListResultRowExperiment(Experiment experiment,
                                   float pvalue, Expression updn) {
        this.experiment = experiment;
        this.pvalue = pvalue;
        this.updn = updn;
    }

    /**
     * Returns p-value
     *
     * @return p-value
     */
    @RestOut(name = "pvalue")
    public float getPvalue() {
        return pvalue;
    }

    /**
     * Returns experiment id
     *
     * @return experiment id
     */
    public long getExperimentId() {
        return experiment.getId();
    }

    /**
     * Return experiment accession
     *
     * @return experiment accession
     */
    @RestOut(name = "accession")
    public String getExperimentAccession() {
        return experiment.getAccession();
    }

    /**
     * Returns experiment description
     *
     * @return experiment description
     */
    public String getExperimentDescription() {
        return experiment.getDescription();
    }

    /**
     * Returns up or down
     *
     * @return UP or DOWN
     */
    @RestOut(name = "expression")
    public Expression getUpdn() {
        return updn;
    }
}

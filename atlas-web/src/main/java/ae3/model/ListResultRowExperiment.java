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
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

/**
 * @author pashky
 */
public class ListResultRowExperiment {
    private final UpDownExpression updn;
    private final float pvalue;
    private final Experiment experiment;
    private final String deAccession;

    /**
     * @param experiment  an experiment to show
     * @param pvalue      p-value statistics to show
     * @param deAccession design element accession which this pValue is for
     * @param updn        up or down
     */
    public ListResultRowExperiment(Experiment experiment,
                                   float pvalue,
                                   String deAccession,
                                   UpDownExpression updn) {
        this.experiment = experiment;
        this.pvalue = pvalue;
        this.updn = updn;
        this.deAccession = deAccession;
    }

    public float getPvalue() {
        return pvalue;
    }

    public String getExperimentAccession() {
        return experiment.getAccession();
    }

    public String getExperimentDescription() {
        return experiment.getDescription();
    }

    public UpDownExpression getUpdn() {
        return updn;
    }

    public String getDeAccession() {
        return deAccession;
    }
}

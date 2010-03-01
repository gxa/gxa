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

package ae3.model;

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;

/**
 * @author pashky
 */
public class ListResultRowExperiment {
    private long experimentId;
    private String experimentAccession;
    private String experimentDescription;
    private Expression updn;
    private double pvalue;

    /**
     * Constructor
     * @param experimentId experiment id
     * @param experimentAccession experiment accessment
     * @param experimentDescription experimnet description
     * @param pvalue p-value
     * @param updn up or down
     */
    public ListResultRowExperiment(long experimentId, String experimentAccession, String experimentDescription,
                                   double pvalue, Expression updn) {
        this.experimentId = experimentId;
        this.experimentAccession = experimentAccession;
        this.experimentDescription = experimentDescription;
        this.pvalue = pvalue;
        this.updn = updn;
    }

    /**
     * Returns p-value
     * @return p-value
     */
    @RestOut(name="pvalue")
    public double getPvalue() {
        return pvalue;
    }

    /**
     * Returns experiment id
     * @return experiment id
     */
    public long getExperimentId() {
        return experimentId;
    }

    /**
     * Return experiment accession
     * @return experiment accession
     */
    @RestOut(name="accession")
    public String getExperimentAccession() {
        return experimentAccession;
    }

    /**
     * Returns experiment description
     * @return experiment description
     */
    public String getExperimentDescription() {
        return experimentDescription;
    }

    /**
     * Returns up or down
     * @return UP or DOWN
     */
    @RestOut(name="expression")
    public Expression getUpdn() {
        return updn;
    }
}

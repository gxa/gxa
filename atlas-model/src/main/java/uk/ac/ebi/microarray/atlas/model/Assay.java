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

package uk.ac.ebi.microarray.atlas.model;

import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.Temporary;

public class Assay extends ObjectWithProperties  {
    private long assayID;
    private final String accession;
    private Experiment experiment;
    private ArrayDesign arrayDesign;

    public Assay(String accession) {
        this.accession = accession;
    }

    public String getAccession() {
        return accession;
    }

    @Temporary
    @Deprecated
    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    @Temporary
    @Deprecated
    public void setArrayDesign(ArrayDesign arrayDesign) {
        this.arrayDesign = arrayDesign;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    public long getAssayID() {
        return assayID;
    }

    public void setAssayID(long assayID) {
        this.assayID = assayID;
    }

    @Override
    public String toString() {
        return "Assay{" +
                "accession='" + getAccession() + '\'' +
                ", experiment='" + experiment + '\'' +
                ", arrayDesign='" + arrayDesign + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Assay && ((Assay) o).assayID == assayID;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(assayID).hashCode();
    }
}

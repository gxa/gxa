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

import uk.ac.ebi.gxa.impl.AssayImpl;

public class Assay extends AssayImpl {
    private String experimentAccession;
    private String arrayDesignAccession;
    private long assayID;

    public Assay(String accession) {
        super(accession);
    }

    public String getExperimentAccession() {
        return experimentAccession;
    }

    public void setExperimentAccession(String experimentAccession) {
        this.experimentAccession = experimentAccession;
    }

    public String getArrayDesignAccession() {
        return arrayDesignAccession;
    }

    public void setArrayDesignAccession(String arrayDesignAccession) {
        this.arrayDesignAccession = arrayDesignAccession;
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
                ", experimentAccession='" + experimentAccession + '\'' +
                ", arrayDesignAcession='" + arrayDesignAccession + '\'' +
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

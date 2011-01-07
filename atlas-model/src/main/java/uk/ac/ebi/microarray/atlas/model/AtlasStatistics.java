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

/**
 * @author Tony Burdett
 */
public class AtlasStatistics {
    private String dataRelease;
    private int experimentCount;
    private int assayCount;
    private int geneCount;
    private int propertyValueCount;
    private int newExperimentCount;
    private int factorValueCount;

    public int getExperimentCount() {
        return experimentCount;
    }

    public void setExperimentCount(int experimentCount) {
        this.experimentCount = experimentCount;
    }

    public int getAssayCount() {
        return assayCount;
    }

    public void setAssayCount(int assayCount) {
        this.assayCount = assayCount;
    }

    public int getGeneCount() {
        return geneCount;
    }

    public void setGeneCount(int geneCount) {
        this.geneCount = geneCount;
    }

    public int getPropertyValueCount() {
        return propertyValueCount;
    }

    public void setPropertyValueCount(int propertyValueCount) {
        this.propertyValueCount = propertyValueCount;
    }

    public String getDataRelease() {
        return dataRelease;
    }

    public void setDataRelease(String dataRelease) {
        this.dataRelease = dataRelease;
    }

    public int getNewExperimentCount() {
        return newExperimentCount;
    }

    public void setNewExperimentCount(int newExperimentCount) {
        this.newExperimentCount = newExperimentCount;
    }

    public void setFactorValueCount(int factorValueCount) {
        this.factorValueCount = factorValueCount;
    }

    public int getFactorValueCount() {
        return factorValueCount;
    }
}

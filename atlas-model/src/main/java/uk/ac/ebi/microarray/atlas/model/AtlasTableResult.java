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
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 06-Nov-2009
 */
public class AtlasTableResult {
    private long experimentID;
    private long geneID;
    private String property;
    private String propertyValue;
    private String upOrDown;
    private float pValAdj;

    public long getExperimentID() {
        return experimentID;
    }

    public void setExperimentID(long experimentID) {
        this.experimentID = experimentID;
    }

    public long getGeneID() {
        return geneID;
    }

    public void setGeneID(long geneID) {
        this.geneID = geneID;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getUpOrDown() {
        return upOrDown;
    }

    public void setUpOrDown(String upOrDown) {
        this.upOrDown = upOrDown;
    }

    public float getPValAdj() {
        return pValAdj;
    }

    public void setPValAdj(float pValAdj) {
        this.pValAdj = pValAdj;
    }
}

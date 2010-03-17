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
 * @date 25-Sep-2009
 */
public class OntologyMapping {
    private Long experimentId;
    private String experimentAccession;
    private String property;
    private String propertyValue;
    private String ontologyTerm;
    private String ontologyTermName;
    private String ontologyTermID;
    private String ontologyName;
    private boolean isSampleProperty;
    private boolean isAssayProperty;
    private boolean isFactorValue;

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public String getExperimentAccession() {
        return experimentAccession;
    }

    public void setExperimentAccession(String experimentAccession) {
        this.experimentAccession = experimentAccession;
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

    public String getOntologyTerm() {
        return ontologyTerm;
    }

    public void setOntologyTerm(String ontologyTerm) {
        this.ontologyTerm = ontologyTerm;
    }

    public String getOntologyTermName() {
        return ontologyTermName;
    }

    public void setOntologyTermName(String ontologyTermName) {
        this.ontologyTermName = ontologyTermName;
    }

    public String getOntologyTermID() {
        return ontologyTermID;
    }

    public void setOntologyTermID(String ontologyTermID) {
        this.ontologyTermID = ontologyTermID;
    }

    public String getOntologyName() {
        return ontologyName;
    }

    public void setOntologyName(String ontologyName) {
        this.ontologyName = ontologyName;
    }

    public boolean isSampleProperty() {
        return isSampleProperty;
    }

    public void setSampleProperty(boolean sampleProperty) {
        isSampleProperty = sampleProperty;
    }

    public boolean isAssayProperty() {
        return isAssayProperty;
    }

    public void setAssayProperty(boolean assayProperty) {
        isAssayProperty = assayProperty;
    }

    public boolean isFactorValue() {
        return isFactorValue;
    }

    public void setFactorValue(boolean factorValue) {
        isFactorValue = factorValue;
    }
}

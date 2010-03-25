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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: Andrey Date: Aug 27, 2009 Time: 10:31:25 AM To change this template use File |
 * Settings | File Templates.
 */
public class Assay implements ObjectWithProperties {
    private String accession;
    private String experimentAccession;
    private String arrayDesignAccession;
    private List<Property> properties;
    private int assayID;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
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

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public int getAssayID() {
        return assayID;
    }

    public void setAssayID(int assayID) {
        this.assayID = assayID;
    }

    /**
     * Convenience method for adding properties to an Assay
     *
     * @param accession     the property accession to set
     * @param name          the property name
     * @param value         the property value
     * @param isFactorValue whether this property is a factor value or not
     * @return the resulting property
     */
    public Property addProperty(String accession, String name, String value,
                                boolean isFactorValue) {
        Property result = new Property();
        result.setAccession(accession);
        result.setName(name);
        result.setValue(value);
        result.setFactorValue(isFactorValue);

        if (null == properties) {
            properties = new ArrayList<Property>();
        }

        properties.add(result);

        return result;
    }

    public boolean addProperty(Property p) {
        if (null == properties) {
            properties = new ArrayList<Property>();
        }

        return properties.add(p);
    }

    @Override
    public String toString() {
        return "Assay{" +
                "accession='" + accession + '\'' +
                ", experimentAccession='" + experimentAccession + '\'' +
                ", arrayDesignAcession='" + arrayDesignAccession + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Assay assay = (Assay) o;

        if (accession != null ? !accession.equals(assay.accession) : assay.accession != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = accession != null ? accession.hashCode() : 0;
        return result;
    }
}

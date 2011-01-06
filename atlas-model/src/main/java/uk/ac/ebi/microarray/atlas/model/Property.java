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

public class Property {
    private long propertyId;
    private long propertyValueId;
    private String accession;
    private String name;
    private String value;
    private String efoTerms; //comma separated EFO terms
    private boolean isFactorValue;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isFactorValue() {
        return isFactorValue;
    }

    public void setFactorValue(boolean factorValue) {
        isFactorValue = factorValue;
    }

    public long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(long propertyId) {
        this.propertyId = propertyId;
    }

    public long getPropertyValueId() {
        return propertyValueId;
    }

    public void setPropertyValueId(long propertyValueId) {
        this.propertyValueId = propertyValueId;
    }

    public String getEfoTerms() {
        if (null == efoTerms)
            return "";
        return efoTerms;
    }

    public void setEfoTerms(String value) {
        this.efoTerms = value;
    }

    @Override
    public String toString() {
        return "Property{" +
                "propertyId=" + propertyId +
                ", propertyValueId=" + propertyValueId +
                ", accession='" + accession + '\'' +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", isFactorValue=" + isFactorValue +
                '}';
    }
}

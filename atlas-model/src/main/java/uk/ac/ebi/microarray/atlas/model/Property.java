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
    private PropertyValue propertyValue = new PropertyValue(0L, new PropertyDefinition(0L, null), null);
    private String efoTerms; //comma separated EFO terms

    public Property() {
    }

    public String getName() {
        return propertyValue.getDefinition().getName();
    }

    public void setName(String name) {
        propertyValue = propertyValue.withDefinition(propertyValue.getDefinition().withName(name));
    }

    public String getValue() {
        return propertyValue.getValue();
    }

    public void setValue(String value) {
        propertyValue = propertyValue.withValue(value);
    }

    public long getPropertyId() {
        return propertyValue.getDefinition().getId();
    }

    public void setPropertyId(long propertyId) {
        propertyValue = propertyValue.withDefinition(propertyValue.getDefinition().withId(propertyId));
    }

    public long getPropertyValueId() {
        return propertyValue.getId();
    }

    public void setPropertyValueId(long propertyValueId) {
        propertyValue = propertyValue.withId(propertyValueId);
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
        return propertyValue.toString() + ",efo=" + efoTerms;
    }
}

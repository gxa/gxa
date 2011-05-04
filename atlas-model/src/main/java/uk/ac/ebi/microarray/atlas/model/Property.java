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

import javax.annotation.concurrent.Immutable;

@Immutable
public final class Property {
    private final PropertyValue propertyValue;
    private final String efoTerms; // TODO: 4alf: comma separated EFO terms - replace with a collection

    public Property(String name, String value, String efoTerms) {
        propertyValue = new PropertyValue(null, new PropertyDefinition(null, name), value);
        this.efoTerms = efoTerms;
    }

    public Property(PropertyValue pv, String efoTerms) {
        propertyValue = pv;
        this.efoTerms = efoTerms;
    }

    public String getName() {
        return propertyValue.getDefinition().getName();
    }

    public String getValue() {
        return propertyValue.getValue();
    }

    public long getPropertyId() {
        return propertyValue.getDefinition().getId();
    }

    public long getPropertyValueId() {
        return propertyValue.getId();
    }

    public String getEfoTerms() {
        return null == efoTerms ? "" : efoTerms;
    }

    @Override
    public String toString() {
        return "Property{" +
                "propertyValue=" + propertyValue +
                ", efoTerms='" + efoTerms + '\'' +
                '}';
    }
}

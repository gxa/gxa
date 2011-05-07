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
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Joiner.on;
import static java.util.Collections.unmodifiableList;

@Immutable
public final class Property {
    private final Long id;
    private final ObjectWithProperties owner;
    private final PropertyValue propertyValue;
    private final List<OntologyTerm> terms;

    public Property(ObjectWithProperties owner, String name, String value, List<OntologyTerm> efoTerms) {
        this.id = null; // TODO: 4alf: we must handle this on save
        this.owner = owner;
        propertyValue = new PropertyValue(null, new PropertyDefinition(null, name), value);
        this.terms = new ArrayList<OntologyTerm>(efoTerms);
    }

    public Property(Long id, ObjectWithProperties owner, PropertyValue pv, List<OntologyTerm> efoTerms) {
        this.id = id;
        this.owner = owner;
        propertyValue = pv;
        this.terms = new ArrayList<OntologyTerm>(efoTerms);
    }

    public Long getId() {
        return id;
    }

    public ObjectWithProperties getOwner() {
        return owner;
    }

    public String getName() {
        return propertyValue.getDefinition().getName();
    }

    public String getValue() {
        return propertyValue.getValue();
    }

    public PropertyValue getPropertyValue() {
        return propertyValue;
    }

    public List<OntologyTerm> getTerms() {
        return unmodifiableList(terms);
    }

    @Deprecated
    public long getPropertyId() {
        return propertyValue.getDefinition().getId();
    }

    @Deprecated
    public long getPropertyValueId() {
        return propertyValue.getId();
    }

    @Deprecated
    public String getEfoTerms() {
        return on(',').join(terms);
    }

    @Override
    public String toString() {
        return "Property{" +
                "propertyValue=" + propertyValue +
                ", terms='" + terms + '\'' +
                '}';
    }
}

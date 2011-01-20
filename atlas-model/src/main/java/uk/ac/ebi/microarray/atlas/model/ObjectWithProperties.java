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

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;

/**
 * Base class for something having properties, like assays and samples
 *
 * @author pashky
 */
public abstract class ObjectWithProperties {
    private List<Property> properties = new ArrayList<Property>();

    public String getPropertySummary(final String name) {
        return on(",").join(transform(
                filter(properties,
                        new Predicate<Property>() {
                            public boolean apply(@Nonnull Property input) {
                                return input.getName().equals(name);
                            }
                        }),
                new Function<Property, String>() {
                    public String apply(@Nonnull Property input) {
                        return input.getValue();
                    }
                }
        ));
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = new ArrayList<Property>(properties);
    }

    /**
     * Convenience method for adding a property to this object.
     *
     * @param accession     the accession of the property
     * @param value         the value of the property
     * @param isFactorValue whether this property is a factor value or not
     * @param efoTerms      ontology terms
     * @return the resulting property
     */
    public Property addProperty(String accession, String value,
                                boolean isFactorValue, String efoTerms) {
        Property result = new Property();
        result.setAccession(accession);
        result.setName(accession);
        result.setValue(value);
        result.setFactorValue(isFactorValue);
        result.setEfoTerms(efoTerms);
        properties.add(result);
        return result;
    }

    public boolean addProperty(Property p) {
        return properties.add(p);
    }
}

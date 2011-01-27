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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.transform;

/**
 * Base class for something having properties, like assays and samples
 *
 * @author pashky
 */
public abstract class ObjectWithProperties {
    @Nonnull
    private ListMultimap<String, Property> properties = ArrayListMultimap.create();

    public boolean hasNoProperties() {
        return properties.isEmpty();
    }

    public String getPropertySummary(final String name) {
        return prepareSummary(name, GET_VALUE);
    }

    public String getEfoSummary(final String name) {
        return prepareSummary(name, GET_EFO_TERMS);
    }

    private String prepareSummary(String name, Function<Property, String> function) {
        return on(",").join(transform(getProperties(name), function));
    }

    @Nonnull
    public List<Property> getProperties(String name) {
        return properties.get(name.toLowerCase());
    }

    @Nonnull
    public List<Property> getProperties() {
        return new ArrayList<Property>(properties.values());
    }

    public Collection<String> getPropertyNames() {
        return new ArrayList<String>(properties.asMap().keySet());
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
        properties.put(result.getName(), result);
        return result;
    }

    public boolean addProperty(Property p) {
        if (p == null)
            throw new IllegalArgumentException("Property should not be null");
        return registerProperty(p);
    }

    private boolean registerProperty(@Nonnull Property p) {
        return properties.put(p.getName().toLowerCase(), p);
    }

    private static final Function<Property, String> GET_EFO_TERMS = new Function<Property, String>() {
        public String apply(@Nonnull Property input) {
            return input.getEfoTerms();
        }
    };

    private static final Function<Property, String> GET_VALUE = new Function<Property, String>() {
        public String apply(@Nonnull Property input) {
            return input.getValue();
        }
    };
}

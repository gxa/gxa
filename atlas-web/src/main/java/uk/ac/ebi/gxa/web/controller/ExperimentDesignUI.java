/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.web.controller;

import uk.ac.ebi.gxa.utils.LazyMap;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.*;

/**
 * @author alf
 */
public class ExperimentDesignUI {
    private final Experiment exp;

    public ExperimentDesignUI(Experiment exp) {
        this.exp = exp;
    }

    public SortedSet<Property> getProperties() {
        return exp.getProperties();
    }

    public List<Assay> getAssays() {
        return exp.getAssays();
    }

    public Map<Property, Map<Assay, Collection<PropertyValue>>> getValues() {
        // we can rewrite it using Maps.uniqueIndex(), but it doesn't seems to be easier or more concise
        return new LazyMap<Property, Map<Assay, Collection<PropertyValue>>>() {
            @Override
            protected Map<Assay, Collection<PropertyValue>> map(final Property property) {
                return new LazyMap<Assay, Collection<PropertyValue>>() {
                    @Override
                    protected Collection<PropertyValue> map(Assay assay) {
                        return assay.getEffectiveValues(property);
                    }

                    @Override
                    protected Iterator<Assay> keys() {
                        return exp.getAssays().iterator();
                    }
                };
            }

            @Override
            protected Iterator<Property> keys() {
                return exp.getProperties().iterator();
            }
        };
    }
}

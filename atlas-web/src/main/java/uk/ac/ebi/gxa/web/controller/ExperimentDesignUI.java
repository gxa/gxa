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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.utils.LazyMap;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.collect.Collections2.transform;

/**
 * @author alf
 */
public class ExperimentDesignUI {
    private final Experiment exp;
    private final SortedSet<Property> expProperties;
    private final Map<Property, Map<Assay, Collection<String>>> expValues;

    private int limit = -1;
    private int offset = -1;

    public ExperimentDesignUI(Experiment experiment) {
        this(experiment, -1, -1);
    }

    public ExperimentDesignUI(Experiment experiment, int offset, int limit) {
        this.offset = offset;
        this.limit = limit;
        this.exp = experiment;
        this.expProperties = experiment.getProperties();
        this.expValues = new LazyMap<Property, Map<Assay, Collection<String>>>() {
            @Override
            protected Map<Assay, Collection<String>> map(final Property property) {
                return new LazyMap<Assay, Collection<String>>() {
                    @Override
                    protected Collection<String> map(Assay assay) {
                        return transform(assay.getEffectiveValues(property), new Function<PropertyValue, String>() {
                            @Override
                            public String apply(@Nullable PropertyValue input) {
                                return input.getDisplayValue();
                            }
                        });
                    }

                    @Override
                    protected Iterator<Assay> keys() {
                        return exp.getAssays().iterator();
                    }
                };
            }

            @Override
            protected Iterator<Property> keys() {
                return expProperties.iterator();
            }
        };
    }

    public Collection<String> getPropertyNames() {
        return transform(expProperties, new Function<Property, String>() {
            @Override
            public String apply(@Nullable Property input) {
                return input.getDisplayName();
            }
        });
    }

    public Collection<Row> getPropertyValues() {
        return transform(getAssays(), new Function<Assay, Row>() {
            @Override
            public Row apply(@Nullable Assay input) {
                return new Row(input);
            }
        });
    }

    public int getTotal() {
        return exp.getAssays().size();
    }

    /**
     * A quick workaround for using the object outside of the session; calling for all lazy methods.
     *
     * @return the current instance of {@link ExperimentDesignUI}
     */
    public ExperimentDesignUI unlazy() {
        getPropertyNames();
        for (Row r : getPropertyValues()) {
            r.getPropertyValues();
        }
        return this;
    }

    private List<Assay> getAssays() {
        List<Assay> assays = exp.getAssays();
        return (offset >= 0 && limit >= 0) ?
                assays.subList(offset, Math.min(offset + limit, assays.size())) : assays;
    }

    public class Row {
        private final Assay assay;
        private List<String> values;

        public Row(Assay assay) {
            this.assay = assay;
        }

        public String getAssayAcc() {
            return assay.getAccession();
        }

        public String getArrayDesignAcc() {
            return assay.getArrayDesign().getAccession();
        }

        public List<String> getPropertyValues() {
            if (values == null) {
                values = new ArrayList<String>();
                for (Property p : expProperties) {
                    values.add(Joiner.on(",").join(expValues.get(p).get(assay)));
                }
            }
            return Collections.unmodifiableList(values);
        }
    }
}

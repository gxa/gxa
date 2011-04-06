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

package uk.ac.ebi.gxa.requesthandlers.api.v2;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.util.*;

class AssaysQueryHandler implements QueryHandler {
    private final AtlasDAO atlasDao;

    AssaysQueryHandler(AtlasDAO atlasDao) {
        this.atlasDao = atlasDao;
    }

    private static class PropertyDecorator {
        private final Property property;

        PropertyDecorator(Property property) {
            this.property = property;
        }

        public String getValue() {
            return property.getValue();
        }

        public String getEfoTerms() {
            return property.getEfoTerms();
        }
    }

    private static class AssayDecorator {
        private final Assay assay;

        AssayDecorator(Assay assay) {
            this.assay = assay;
        }

        public String getAccession() {
            return assay.getAccession();
        }

        public String getArrayDesignAccession() {
            return assay.getArrayDesignAccession();
        }

        public Map<String,List<PropertyDecorator>> getProperties() {
            final Map<String,List<PropertyDecorator>> propertyMap =
                new TreeMap<String,List<PropertyDecorator>>();
            for (final String name : assay.getPropertyNames()) {
                final List<PropertyDecorator> properties = new LinkedList<PropertyDecorator>();
                for (final Property property : assay.getProperties(name)) {
                    properties.add(new PropertyDecorator(property));
                }
                if (properties.size() > 0) {
                    propertyMap.put(name, properties);
                }
            }
            return propertyMap;
        }
    }

    public Object getResponse(Map query) {
        final Object experimentAccession = query.get("experimentAccession");
        if (experimentAccession == null) {
            return new Error("Experiment accession is not specified");
        } else if (!(experimentAccession instanceof String)) {
            return new Error("Experiment accession must be a string");
        }

        final List<Assay> assays = atlasDao.getAssaysByExperimentAccession((String)experimentAccession);
        if (assays == null || assays.size() == 0) {
            return new Error("Assays for experiment " + experimentAccession + " not found");
        }
        final List<AssayDecorator> decorators = new ArrayList<AssayDecorator>(assays.size());
        for (final Assay a : assays) {
            decorators.add(new AssayDecorator(a));
        }
        return decorators;
    }
}

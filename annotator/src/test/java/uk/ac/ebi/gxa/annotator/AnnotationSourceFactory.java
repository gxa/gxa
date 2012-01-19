/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator;

import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.annotation.Nonnull;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * @author Olga Melnichuk
 */
public class AnnotationSourceFactory {

    public static BioMartAnnotationSourceBuilder newBioMartAnnotationSource() {
        return new BioMartAnnotationSourceBuilder();
    }

    public static class BioMartAnnotationSourceBuilder {
        private Organism organism;
        private Software software;
        private final Map<String, BioEntityType> types = new LinkedHashMap<String, BioEntityType>();
        private final Map<String, BioEntityProperty> properties = new LinkedHashMap<String, BioEntityProperty>();
        private final Map<String, ExternalBioEntityProperty> extProperties = new LinkedHashMap<String, ExternalBioEntityProperty>();

        private BioEntityProperty getOrCreateProperty(String name) {
            BioEntityProperty prop = properties.get(name);
            if (prop == null) {
                prop = new BioEntityProperty(null, name);
                properties.put(name, prop);
            }
            return prop;
        }

        private Collection<ExternalBioEntityProperty> getExternalProperties() {
            return extProperties.isEmpty() ? newExternalProperties() : extProperties.values();
        }

        private Collection<BioEntityType> getBioEntityTypes() {
            List<BioEntityType> beTypes = new ArrayList<BioEntityType>();
            if (!types.isEmpty()) {
                for (BioEntityType type : types.values()) {
                    if (extProperties.containsKey(type.getIdentifierProperty().getName()) &&
                            extProperties.containsKey(type.getNameProperty().getName())) {
                        beTypes.add(type);
                    } else {
                        throw new IllegalStateException("Bad type config: " + type);
                    }
                }
                return types.values();
            }

            if (!extProperties.isEmpty()) {
                Iterator<ExternalBioEntityProperty> iter = extProperties.values().iterator();
                ExternalBioEntityProperty prop1 = iter.next();
                if (!iter.hasNext()) {
                    property(prop1.getName() + "1", prop1.getBioEntityProperty().getName() + "1");
                }

                iter = extProperties.values().iterator();
                prop1 = iter.next();
                ExternalBioEntityProperty prop2 = iter.next();
                beTypes.add(new BioEntityType(null, "aType", 0, prop1.getBioEntityProperty(), prop2.getBioEntityProperty()));
                return beTypes;
            }

            return newBioEntityTypes();
        }

        /**
         * Creates an organism to be added to the annotation source.
         * Optional: if no organism set, the default one will be created.
         *
         * @param orgName an organism name
         * @return the original annotation builder instance
         */
        public BioMartAnnotationSourceBuilder organism(@Nonnull String orgName) {
            this.organism = new Organism(null, orgName);
            return this;
        }

        /**
         * Creates a software object to be added to the annotation source.
         * Optional: if no software set, the default one will be created.
         *
         * @param name    a name of software
         * @param version a version of software
         * @return the original annotation builder instance
         */
        public BioMartAnnotationSourceBuilder software(@Nonnull String name, @Nonnull String version) {
            this.software = new Software(name, version);
            return this;
        }

        /**
         * Creates BioEntityType object to be added to the annotation source.
         * Optional: if no types set, the default some will be created.
         *
         * @param typeName     a type name
         * @param idPropName   an Identifier property name
         * @param namePropName a Name property name
         * @return the original annotation builder instance
         */
        public BioMartAnnotationSourceBuilder type(@Nonnull String typeName, @Nonnull String idPropName, @Nonnull String namePropName) {
            if (!types.containsKey(typeName)) {
                BioEntityProperty idProp = getOrCreateProperty(idPropName);
                BioEntityProperty nameProp = getOrCreateProperty(namePropName);
                types.put(typeName, new BioEntityType(null, typeName, 0, idProp, nameProp));
            }
            return this;
        }

        /**
         * Creates ExternalBioEntityProperty object to be added to the annotation source.
         * Optional: if no properties set, the default some will be created.
         *
         * @param name    a property name
         * @param extName a corresponding external property name
         * @return the original annotation builder instance
         */
        public BioMartAnnotationSourceBuilder property(@Nonnull String name, @Nonnull String extName) {
            BioEntityProperty prop = getOrCreateProperty(name);
            ExternalBioEntityProperty extProp = new ExternalBioEntityProperty(extName, prop, null);
            extProperties.put(name, extProp);
            return this;
        }

        public BioMartAnnotationSource create() {
            Organism org = newOrganism(organism);
            Software sw = newSoftware(software);

            BioMartAnnotationSource annotSource = new BioMartAnnotationSource(sw, org);
            Collection<ExternalBioEntityProperty> extProps = getExternalProperties();
            for (ExternalBioEntityProperty extProp : extProps) {
                extProp.setAnnotationSrc(annotSource);
                annotSource.addExternalProperty(extProp);
            }

            Collection<BioEntityType> types = getBioEntityTypes();
            for (BioEntityType type : types) {
                annotSource.addBioEntityType(type);
            }
            return annotSource;
        }
    }

    private static Map<String, BioEntityProperty> properties = new HashMap<String, BioEntityProperty>() {
        {
            put("ensgene", new BioEntityProperty(null, "ensgene"));
            put("enstranscript", new BioEntityProperty(null, "enstranscript"));
            put("name", new BioEntityProperty(null, "name"));
            put("identifier", new BioEntityProperty(null, "identifier"));
            put("go", new BioEntityProperty(null, "go"));

        }
    };

    private static List<ExternalBioEntityProperty> newExternalProperties() {
        return asList(
                new ExternalBioEntityProperty("ensgene", properties.get("ensgene"), null),
                new ExternalBioEntityProperty("enstranscript", properties.get("enstranscript"), null),
                new ExternalBioEntityProperty("symbol", properties.get("name"), null),
                new ExternalBioEntityProperty("identifier", properties.get("identifier"), null),
                new ExternalBioEntityProperty("go_id", properties.get("go"), null)
        );
    }

    private static List<BioEntityType> newBioEntityTypes() {
        return asList(
                new BioEntityType(null, "ensgene", 1, properties.get("ensgene"), properties.get("name")),
                new BioEntityType(null, "enstranscript", 0, properties.get("enstranscript"), properties.get("identifier"))
        );
    }

    private static Organism newOrganism(Organism source) {
        return source == null ? new Organism(null, "arabidopsis thaliana") : source;
    }

    private static Software newSoftware(Software source) {
        return source == null ? new Software("plants", "8") : source;
    }
}

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

package uk.ac.ebi.microarray.atlas.model;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class BioEntity {
    private long id;
    private String identifier;
    private String name;
    private String type;
    private List<BEPropertyValue> properties = new ArrayList<BEPropertyValue>();

    private String species;

    public static final String NAME_PROPERTY_SYMBOL = "Symbol";
    public static final String NAME_PROPERTY_MIRBASE = "miRBase: Accession Number";

    public BioEntity(String identifier) {
        this.identifier = identifier;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<BEPropertyValue> getProperties() {
        return unmodifiableList(properties);
    }

    public boolean addProperty(BEPropertyValue p) {
        return properties.add(p);
    }

    public void clearProperties() {
        properties.clear();
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getName() {
        if (StringUtils.isEmpty(name)) {
            name = identifier;
            for (BEPropertyValue property : properties) {
                if (NAME_PROPERTY_SYMBOL.equalsIgnoreCase(property.getName())) {
                    name = property.getValue();
                    break;
                } else if (NAME_PROPERTY_MIRBASE.equalsIgnoreCase(property.getName())) {
                    name = property.getValue();
                    break;
                }
            }
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioEntity bioEntity = (BioEntity) o;

        return identifier.equals(bioEntity.identifier) &&
                !(species != null ? !species.equals(bioEntity.species) : bioEntity.species != null) &&
                !(type != null ? !type.equals(bioEntity.type) : bioEntity.type != null);

    }

    @Override
    public int hashCode() {
        int result = identifier.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (species != null ? species.hashCode() : 0);
        return result;
    }
}

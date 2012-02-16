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

package uk.ac.ebi.microarray.atlas.model.bioentity;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.microarray.atlas.model.Organism;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class BioEntity {
    private Long id;
    private String identifier;
    private String name;
    private BioEntityType type;
    private List<BEPropertyValue> properties = new ArrayList<BEPropertyValue>();

    private Organism organism;

    public BioEntity(String identifier, BioEntityType type, Organism organism) {
        this.identifier = identifier;
        this.type = type;
        this.organism = organism;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public BioEntityType getType() {
        return type;
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

    public Organism getOrganism() {
        return organism;
    }


    public String getName() {
        if (StringUtils.isEmpty(name)){
            name = identifier;
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

        if (!identifier.equals(bioEntity.identifier)) return false;
        if (name != null ? !name.equals(bioEntity.name) : bioEntity.name != null) return false;
        if (!organism.equals(bioEntity.organism)) return false;
        if (!type.equals(bioEntity.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = identifier.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + type.hashCode();
        result = 31 * result + organism.hashCode();
        return result;
    }
}

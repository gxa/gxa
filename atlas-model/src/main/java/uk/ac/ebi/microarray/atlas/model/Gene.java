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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableList;

public class Gene {
    private String identifier;
    private String name;
    private String species;
    private List<Property> properties = new ArrayList<Property>();
    private long geneID;
    private Set<Long> designElementIDs = new HashSet<Long>();

    public long getGeneID() {
        return geneID;
    }

    public void setGeneID(long geneID) {
        this.geneID = geneID;
    }

    public Set<Long> getDesignElementIDs() {
        return designElementIDs;
    }

    public void setDesignElementIDs(Set<Long> designElementIDs) {
        this.designElementIDs = designElementIDs;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Property> getProperties() {
        return unmodifiableList(properties);
    }

    public boolean addProperty(Property p) {
        return properties.add(p);
    }

    public void clearProperties() {
        properties.clear();
    }
}

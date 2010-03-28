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
import java.util.List;
import java.util.Set;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 24-Sep-2009
 */
public class Gene {
    private String identifier;
    private String name;
    private String species;
    private List<Property> properties;
    private long geneID;
    private Set<Long> designElementIDs;

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
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    /**
     * Convenience method for adding properties to a Gene
     *
     * @param accession     the property accession to set
     * @param name          the property name
     * @param value         the property value
     * @param isFactorValue whether this property is a factor value or not
     * @return the resulting property
     */
    public Property addProperty(String accession, String name, String value,
                                boolean isFactorValue) {
        Property result = new Property();
        result.setAccession(accession);
        result.setName(name);
        result.setValue(value);
        result.setFactorValue(isFactorValue);

        if (null == properties) {
            properties = new ArrayList<Property>();
        }

        properties.add(result);

        return result;
    }

    public boolean addProperty(Property p) {
        if (null == properties) {
            properties = new ArrayList<Property>();
        }

        return properties.add(p);
    }
}

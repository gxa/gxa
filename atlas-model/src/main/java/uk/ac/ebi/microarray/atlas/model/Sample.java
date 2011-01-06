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

public class Sample implements ObjectWithProperties {
    private String accession;
    private List<String> assayAccessions;
    private List<Property> properties;
    private String species;
    private String channel;
    private Long sampleID;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public List<String> getAssayAccessions() {
        return assayAccessions;
    }

    public void setAssayAccessions(List<String> assayAccessions) {
        this.assayAccessions = assayAccessions;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public long getSampleID() {
        return sampleID;
    }

    public void setSampleID(long sampleID) {
        this.sampleID = sampleID;
    }

    /**
     * Convenience method for adding assay accession numbers to this sample,
     * creating links between the two node types.
     *
     * @param assayAccession the assay, listed by accession, this sample links to
     */
    public void addAssayAccession(String assayAccession) {
        if (null == assayAccessions) {
            assayAccessions = new ArrayList<String>();
        }

        assayAccessions.add(assayAccession);
    }

    /**
     * Convenience method for adding a property to this sample.
     *
     * @param accession     the accession of the property
     * @param name          the name of the property
     * @param value         the value of the property
     * @param isFactorValue whether this property is a factor value or not
     * @return the resulting property
     */
    public Property addProperty(String accession, String name, String value,
                                boolean isFactorValue, String efoTerms) {
        Property result = new Property();
        result.setAccession(accession);
        result.setName(name);
        result.setValue(value);
        result.setFactorValue(isFactorValue);
        result.setEfoTerms(efoTerms);

        if (null == properties) {
            properties = new ArrayList<Property>();
        }

        properties.add(result);

        return result;
    }

    public boolean addProperty(Property p) {
        if (properties == null) {
            properties = new ArrayList<Property>();
        }

        return properties.add(p);
    }

    @Override
    public String toString() {
        return "Sample{" +
                "accession='" + accession + '\'' +
                ", assayAccessions=" + assayAccessions +
                ", species='" + species + '\'' +
                ", channel='" + channel + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return sampleID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Sample && ((Sample) o).sampleID.equals(sampleID);
    }
}

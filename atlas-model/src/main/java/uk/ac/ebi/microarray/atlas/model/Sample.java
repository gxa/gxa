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

import uk.ac.ebi.gxa.Experiment;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

public class Sample extends ObjectWithProperties {
    private Long sampleID;
    private Experiment experiment;
    private String accession;
    private Organism organism;
    private String channel;
    private Set<String> assayAccessions = new HashSet<String>();

    public Experiment getExperiment() {
        return experiment;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public void setOrganism(Organism organism) {
        this.organism = organism;
    }

    public Organism getOrganism() {
        return organism;
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
        assayAccessions.add(assayAccession);
    }

    public Set<String> getAssayAccessions() {
        return unmodifiableSet(assayAccessions);
    }

    @Override
    public String toString() {
        return "Sample{" +
                "accession='" + accession + '\'' +
                ", assayAccessions=" + assayAccessions +
                ", organism='" + organism + '\'' +
                ", channel='" + channel + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return sampleID == null ? 0 : sampleID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Sample && ((Sample) o).sampleID.equals(sampleID);
    }
}

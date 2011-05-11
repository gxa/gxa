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

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;

@Entity
public class Assay {
    @Id
    private long assayID;
    private String accession;
    @ManyToOne
    private Experiment experiment;
    @ManyToOne
    private ArrayDesign arrayDesign;
    @ManyToMany
    // TODO: 4alf: this can be expressed in NamingStrategy
    @JoinTable(name = "A2_ASSAYSAMPLE",
            joinColumns = @JoinColumn(name = "ASSAYID", referencedColumnName = "ASSAYID"),
            inverseJoinColumns = @JoinColumn(name = "SAMPLEID", referencedColumnName = "SAMPLEID"))
    private List<Sample> samples = new ArrayList<Sample>();
    @OneToMany(targetEntity = AssayProperty.class, cascade = CascadeType.ALL, mappedBy = "assay")
    private List<AssayProperty> properties = new ArrayList<AssayProperty>();

    Assay() {
    }

    public Assay(long assayID, String accession, Experiment experiment, ArrayDesign arrayDesign) {
        this.assayID = assayID;
        this.accession = accession;
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;
    }

    public Assay(String accession) {
        this.accession = accession;
    }

    public Long getId() {
        return assayID;
    }

    public String getAccession() {
        return accession;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public void setArrayDesign(ArrayDesign arrayDesign) {
        this.arrayDesign = arrayDesign;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    public long getAssayID() {
        return getId();
    }

    public List<Sample> getSamples() {
        return samples;
    }

    @Override
    public String toString() {
        return "Assay{" +
                "accession='" + getAccession() + '\'' +
                ", experiment='" + experiment + '\'' +
                ", arrayDesign='" + arrayDesign + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Assay && ((Assay) o).assayID == assayID;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(assayID).hashCode();
    }

    public List<AssayProperty> getProperties() {
        return properties;
    }

    public void addProperty(String type, String nodeName, String s) {
        properties.add(new AssayProperty(this, type, nodeName, Collections.<OntologyTerm>emptyList()));
    }

    public boolean hasNoProperties() {
        return properties.isEmpty();
    }

    public String getPropertySummary(final String propName) {
        return on(",").join(transform(
                getProperties(propName), new Function<AssayProperty, String>() {
                    @Override
                    public String apply(@Nullable AssayProperty input) {
                        return input.getValue();
                    }
                }
        ));
    }

    public Collection<AssayProperty> getProperties(final String type) {
        return filter(properties, new Predicate<AssayProperty>() {
            @Override
            public boolean apply(@Nonnull AssayProperty input) {
                return input.getName().equals(type);
            }
        });
    }

    public Collection<String> getPropertyNames() {
        return transform(properties,
                new Function<AssayProperty, String>() {
                    @Override
                    public String apply(@Nullable AssayProperty input) {
                        return input.getName();
                    }
                });
    }

    public String getEfoSummary(String name) {
        // TODO: 4alf: implement it!
        return null;  //To change body of created methods use File | Settings | File Templates.
    }
}

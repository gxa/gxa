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
import com.google.common.collect.Collections2;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Sample {
    @Id
    private Long sampleid;
    private String accession;
    @ManyToOne
    private Organism organism;
    private String channel;
    @ManyToOne
    private Experiment experiment;
    @ManyToMany(targetEntity = Assay.class, mappedBy = "samples")
    private List<Assay> assays = new ArrayList<Assay>();
    @OneToMany(targetEntity = SampleProperty.class, cascade = CascadeType.ALL, mappedBy = "sample")
    @Fetch(FetchMode.SUBSELECT)
    private List<SampleProperty> properties = new ArrayList<SampleProperty>();

    public Sample() {
    }

    public Sample(Long id, String accession, Organism organism, String channel) {
        this.sampleid = id;
        this.accession = accession;
        this.organism = organism;
        this.channel = channel;
    }

    public Long getId() {
        return sampleid;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public Organism getOrganism() {
        return organism;
    }

    public String getChannel() {
        return channel;
    }

    public long getSampleID() {
        return getId();
    }


    /**
     * Convenience method for adding assay accession numbers to this sample,
     * creating links between the two node types.
     *
     * @param assayAccession the assay, listed by accession, this sample links to
     */
    public void addAssayAccession(String assayAccession) {

    }

    public Collection<String> getAssayAccessions() {
        return Collections2.transform(assays, new Function<Assay, String>() {
            @Override
            public String apply(@Nonnull Assay assay) {
                return assay.getAccession();
            }
        });
    }

    @Override
    public String toString() {
        return "Sample{" +
                "accession='" + accession + '\'' +
                ", organism='" + organism + '\'' +
                ", channel='" + channel + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return sampleid == null ? 0 : sampleid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Sample && ((Sample) o).sampleid.equals(sampleid);
    }

    public void addAssay(Assay assay) {
        assays.add(assay);
    }

    public List<Assay> getAssays() {
        return assays;
    }

    public List<SampleProperty> getProperties() {
        return properties;
    }

    public void addProperty(String type, String nodeName, String s) {
        properties.add(new SampleProperty(this, type, nodeName, Collections.<OntologyTerm>emptyList()));
    }


    public String getPropertySummary(final String propName) {
        return on(",").join(transform(
                filter(properties,
                        new Predicate<SampleProperty>() {
                            @Override
                            public boolean apply(@Nonnull SampleProperty input) {
                                return input.getName().equals(propName);
                            }
                        }),
                new Function<SampleProperty, String>() {
                    @Override
                    public String apply(@Nullable SampleProperty input) {
                        return input.getValue();
                    }
                }
        ));
    }


    public Collection<String> getPropertyNames() {
        return transform(properties,
                new Function<SampleProperty, String>() {
                    @Override
                    public String apply(@Nullable SampleProperty input) {
                        return input.getName();
                    }
                });
    }

    public String getEfoSummary(final String name) {
        return on(",").join(transform(
                filter(properties,
                        new Predicate<SampleProperty>() {
                            @Override
                            public boolean apply(@Nonnull SampleProperty input) {
                                return input.getName().equals(name);
                            }
                        }),
                new Function<SampleProperty, String>() {
                    @Override
                    public String apply(@Nullable SampleProperty input) {
                        return input.getEfoTerms();
                    }
                }
        ));
    }

    public boolean hasNoProperties() {
        return properties.isEmpty();
    }
}


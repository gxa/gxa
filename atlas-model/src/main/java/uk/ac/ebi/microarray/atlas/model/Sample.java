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
import org.hibernate.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.*;
import java.util.*;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newTreeSet;

@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Sample {
    public static final Logger log = LoggerFactory.getLogger(Sample.class);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sampleSeq")
    @SequenceGenerator(name = "sampleSeq", sequenceName = "A2_SAMPLE_SEQ", allocationSize = 1)
    private Long sampleid;
    private String accession;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private Organism organism;
    private String channel;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private Experiment experiment;
    @ManyToMany(targetEntity = Assay.class, mappedBy = "samples")
    private List<Assay> assays = new ArrayList<Assay>();
    @OneToMany(targetEntity = SampleProperty.class, cascade = CascadeType.ALL, mappedBy = "sample",
            orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<SampleProperty> properties = new ArrayList<SampleProperty>();

    Sample() {
    }

    public Sample(Long id, String accession, Organism organism, String channel) {
        if (accession == null)
            throw new IllegalArgumentException("Cannot add sample with null accession!");
        this.sampleid = id;
        this.accession = accession;
        this.organism = organism;
        this.channel = channel;
    }

    public Sample(String accession) {
        this(null, accession, null, null);
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



    public Long getSampleID() {
        return getId();
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
        if (assays.contains(assay))
            return;

        log.trace("Updating {} with assay accession {}", accession, assay.getAccession());
        assays.add(assay);
        assay.addSample(this);
    }

    public List<Assay> getAssays() {
        return assays;
    }

    public List<SampleProperty> getProperties() {
        return properties;
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
                    public String apply(@Nonnull SampleProperty input) {
                        return input.getValue();
                    }
                }
        ));
    }

    public SortedSet<String> getPropertyNames() {
        return newTreeSet(transform(properties,
                new Function<SampleProperty, String>() {
                    @Override
                    public String apply(@Nonnull SampleProperty input) {
                        return input.getName();
                    }
                }));
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
                    public String apply(@Nonnull SampleProperty input) {
                        return input.getEfoTerms();
                    }
                }
        ));
    }

    public boolean hasNoProperties() {
        return properties.isEmpty();
    }

    public void addProperty(PropertyValue pv) {
        properties.add(new SampleProperty(this, pv));
    }

    public void addProperty(PropertyValue pv, Collection<OntologyTerm> efoTerms) {
        properties.add(new SampleProperty(this, pv, efoTerms));
    }

    public void deleteProperty(final PropertyValue propertyValue) {
        SampleProperty property = getProperty(propertyValue);
        while (property != null) {
            properties.remove(property);
            property = getProperty(propertyValue);
        }
    }

    public void setOrganism(Organism organism) {
        this.organism = organism;
    }

    void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean hasProperty(final PropertyValue propertyValue) {
        for (SampleProperty property : properties) {
            if(property.getPropertyValue().equals(propertyValue))
                return true;
        }

        return false;
    }

    public SampleProperty getProperty(PropertyValue propertyValue) {
        for (SampleProperty property : properties) {
            if(property.getPropertyValue().equals(propertyValue))
                return property;
        }

        return null;
    }

    public void addOrUpdateProperty(PropertyValue propertyValue, List<OntologyTerm> terms) {
        if(!this.hasProperty(propertyValue)) {
            this.addProperty(propertyValue, terms);
        } else {
            SampleProperty sampleProperty = this.getProperty(propertyValue);
            sampleProperty.setTerms(terms);
        }
    }
}


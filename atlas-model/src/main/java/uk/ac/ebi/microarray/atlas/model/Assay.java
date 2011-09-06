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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.*;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.newTreeSet;

@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Assay {
    private static final Function<AssayProperty, String> PROPERTY_NAME =
            new Function<AssayProperty, String>() {
                @Override
                public String apply(@Nonnull AssayProperty input) {
                    return input.getName();
                }
            };
    private static final Function<AssayProperty, String> PROPERTY_VALUE =
            new Function<AssayProperty, String>() {
                @Override
                public String apply(@Nonnull AssayProperty input) {
                    return input.getValue();
                }
            };
    private static final Function<AssayProperty, Collection<OntologyTerm>> PROPERTY_TERMS =
            new Function<AssayProperty, Collection<OntologyTerm>>() {
                @Override
                public Collection<OntologyTerm> apply(@Nonnull AssayProperty input) {
                    return input.getTerms();
                }
            };

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assaySeq")
    @SequenceGenerator(name = "assaySeq", sequenceName = "A2_ASSAY_SEQ", allocationSize = 1)
    private Long assayID;
    private String accession;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private Experiment experiment;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private ArrayDesign arrayDesign;

    @ManyToMany
    // TODO: 4alf: this can be expressed in NamingStrategy
    @JoinTable(name = "A2_ASSAYSAMPLE",
            joinColumns = @JoinColumn(name = "ASSAYID", referencedColumnName = "ASSAYID"),
            inverseJoinColumns = @JoinColumn(name = "SAMPLEID", referencedColumnName = "SAMPLEID"))
    @Fetch(FetchMode.SUBSELECT)
    private List<Sample> samples = new ArrayList<Sample>();

    @OneToMany(targetEntity = AssayProperty.class, mappedBy = "assay",
            orphanRemoval = true, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<AssayProperty> properties = new ArrayList<AssayProperty>();

    Assay() {
    }

    public Assay(Long assayID, String accession, Experiment experiment, ArrayDesign arrayDesign) {
        if (accession == null)
            throw new IllegalArgumentException("Cannot add assay with null accession!");
        this.assayID = assayID;
        this.accession = accession;
        this.experiment = experiment;
        this.arrayDesign = arrayDesign;
    }

    public Assay(String accession) {
        this(null, accession, null, null);
    }

    public Long getId() {
        return assayID;
    }

    public String getAccession() {
        return accession;
    }

    /**
     * @param experiment the new owning experiment
     * @see Experiment#addAssay(Assay)
     */
    void setExperiment(Experiment experiment) {
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

    public Long getAssayID() {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Assay assay = (Assay) o;

        return accession == null ? assay.accession == null : accession.equals(assay.accession);
    }

    @Override
    public int hashCode() {
        return accession != null ? accession.hashCode() : 0;
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
        return on(",").join(transform(getProperties(propName), PROPERTY_VALUE));
    }

    public Collection<AssayProperty> getProperties(final String type) {
        return filter(properties, new PropertyNamePredicate(type));
    }

    public SortedSet<String> getPropertyNames() {
        return newTreeSet(transform(properties, PROPERTY_NAME));
    }

    public String getEfoSummary(String name) {
        return on(",").join(concat(transform(getProperties(name), PROPERTY_TERMS)));
    }

    /**
     * Adds a sample to assay. This method is intentionally package local, please use {@link Sample#addAssay(Assay)}
     * instead - it's a {@link Sample}'s responsibility to update its list of assays.
     *
     * @param sample a sample to add
     */
    void addSample(Sample sample) {
        samples.add(sample);
    }

    public void addProperty(PropertyValue property) {
        properties.add(new AssayProperty(null, this, property, Collections.<OntologyTerm>emptyList()));
    }

    public void addProperty(final PropertyValue property, final List<OntologyTerm> terms) {
        properties.add(new AssayProperty(null, this, property, terms));
    }

    public AssayProperty getProperty(PropertyValue propertyValue) {
        for (AssayProperty property : properties) {
            if (property.getPropertyValue().equals(propertyValue))
                return property;
        }

        return null;
    }

    public boolean hasProperty(final PropertyValue propertyValue) {
        return getProperty(propertyValue) != null;
    }

    public void deleteProperty(final PropertyValue propertyValue) {
        AssayProperty property = getProperty(propertyValue);
        while (property != null) {
            properties.remove(property);
            property = getProperty(propertyValue);
        }
    }

    public void addOrUpdateProperty(PropertyValue propertyValue, List<OntologyTerm> terms) {
        if (!this.hasProperty(propertyValue)) {
            this.addProperty(propertyValue, terms);
        } else {
            AssayProperty assayProperty = this.getProperty(propertyValue);
            assayProperty.setTerms(terms);
        }
    }

    private static class PropertyNamePredicate implements Predicate<AssayProperty> {
        private final String type;

        public PropertyNamePredicate(String type) {
            this.type = type;
        }

        @Override
        public boolean apply(@Nonnull AssayProperty input) {
            return input.getName().equals(type);
        }
    }
}

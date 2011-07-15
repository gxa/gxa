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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.*;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.transform;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

@Entity
@Table(name = "A2_SAMPLEPV")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public final class SampleProperty {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "samplePVSeq")
    @SequenceGenerator(name = "samplePVSeq", sequenceName = "A2_SAMPLEPV_SEQ")
    private Long samplepvid;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private Sample sample;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private PropertyValue propertyValue;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // TODO: 4alf: this can be expressed in NamingStrategy
    @JoinTable(name = "A2_SAMPLEPVONTOLOGY",
            joinColumns = @JoinColumn(name = "SAMPLEPVID", referencedColumnName = "SAMPLEPVID"),
            inverseJoinColumns = @JoinColumn(name = "ONTOLOGYTERMID", referencedColumnName = "ONTOLOGYTERMID"))
    @Fetch(FetchMode.SUBSELECT)
    private Set<OntologyTerm> terms = new HashSet<OntologyTerm>();

    SampleProperty() {
    }

    public SampleProperty(Sample sample, PropertyValue pv) {
        this(sample, pv, Collections.<OntologyTerm>emptyList());
    }

    public SampleProperty(Sample sample, PropertyValue pv, Collection<OntologyTerm> efoTerms) {
        this.sample = sample;
        propertyValue = pv;
        terms.addAll(efoTerms);
    }

    public Long getId() {
        return samplepvid;
    }

    public Sample getOwner() {
        return sample;
    }

    public String getName() {
        return propertyValue.getDefinition().getName();
    }

    public String getValue() {
        return propertyValue.getValue();
    }

    public PropertyValue getPropertyValue() {
        return propertyValue;
    }

    public Set<OntologyTerm> getTerms() {
        return unmodifiableSet(terms);
    }

    @Deprecated
    public long getPropertyId() {
        return propertyValue.getDefinition().getId();
    }

    @Deprecated
    public long getPropertyValueId() {
        return propertyValue.getId();
    }

    @Deprecated
    public String getEfoTerms() {
        return on(',').join(transform(terms, new Function<OntologyTerm, Object>() {
            @Override
            public Object apply(@Nonnull OntologyTerm term) {
                return term.getTerm();
            }
        }));
    }

    @Override
    public String toString() {
        return "SampleProperty{" +
                "propertyValue=" + propertyValue +
                ", terms='" + terms + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SampleProperty)) return false;

        SampleProperty property = (SampleProperty) o;

        if (!propertyValue.equals(property.propertyValue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return propertyValue.hashCode();
    }

    void setSample(Sample sample) {
        this.sample = sample;
    }

    public void setTerms(Set<OntologyTerm> terms) {
        this.terms = terms;
    }
}

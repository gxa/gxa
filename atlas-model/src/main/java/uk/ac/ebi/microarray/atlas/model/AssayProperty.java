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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.*;

import static com.google.common.base.Joiner.on;

@Entity
@Table(name = "A2_ASSAYPV")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public final class AssayProperty {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assayPVSeq")
    @SequenceGenerator(name = "assayPVSeq", sequenceName = "A2_ASSAYPV_SEQ")
    private Long assaypvid;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private Assay assay;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private PropertyValue propertyValue;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "A2_ASSAYPVONTOLOGY",
            joinColumns = @JoinColumn(name = "ASSAYPVID", referencedColumnName = "ASSAYPVID"),
            inverseJoinColumns = @JoinColumn(name = "ONTOLOGYTERMID", referencedColumnName = "ONTOLOGYTERMID"))
    private Set<OntologyTerm> terms = new HashSet<OntologyTerm>();

    AssayProperty() {
    }

    public AssayProperty(Assay assay, String name, String value, Set<OntologyTerm> efoTerms) {
        this.assaypvid = null; // TODO: 4alf: we must handle this on save
        this.assay = assay;
        propertyValue = new PropertyValue(null, new Property(null, name), value);
        this.terms = new HashSet<OntologyTerm>(efoTerms);
    }

    public AssayProperty(Long id, Assay assay, PropertyValue pv, Set<OntologyTerm> efoTerms) {
        this.assaypvid = id;
        this.assay = assay;
        propertyValue = pv;
        this.terms = new HashSet<OntologyTerm>(efoTerms);
    }

    public Long getId() {
        return assaypvid;
    }

    public Assay getOwner() {
        return assay;
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
        return Collections.unmodifiableSet(terms);
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
        return on(',').join(terms);
    }

    @Override
    public String toString() {
        return "AssayProperty{" +
                "propertyValue=" + propertyValue +
                ", terms='" + terms + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssayProperty)) return false;

        AssayProperty property = (AssayProperty) o;

        if (!propertyValue.equals(property.propertyValue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return propertyValue.hashCode();
    }

    public void setTerms(Set<OntologyTerm> terms) {
        this.terms = terms;
    }
}

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
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.transform;
import static java.util.Collections.unmodifiableList;

@Entity
@Table(name = "A2_ASSAYPV")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public final class AssayProperty {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "assayPVSeq")
    @SequenceGenerator(name = "assayPVSeq", sequenceName = "A2_ASSAYPV_SEQ", allocationSize = 1)
    private Long assaypvid;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private Assay assay;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private PropertyValue propertyValue;
    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinTable(name = "A2_ASSAYPVONTOLOGY",
            joinColumns = @JoinColumn(name = "ASSAYPVID", referencedColumnName = "ASSAYPVID"),
            inverseJoinColumns = @JoinColumn(name = "ONTOLOGYTERMID", referencedColumnName = "ONTOLOGYTERMID"))
    private List<OntologyTerm> terms = new ArrayList<OntologyTerm>();

    AssayProperty() {
    }

    public AssayProperty(Assay assay, String name, String value, List<OntologyTerm> efoTerms) {
        this.assaypvid = null; // TODO: 4alf: we must handle this on save
        this.assay = assay;
        propertyValue = new PropertyValue(null, new Property(null, name), value);
        this.terms = new ArrayList<OntologyTerm>(efoTerms);
    }

    public AssayProperty(Long id, Assay assay, PropertyValue pv, List<OntologyTerm> efoTerms) {
        this.assaypvid = id;
        this.assay = assay;
        propertyValue = pv;
        this.terms = new ArrayList<OntologyTerm>(efoTerms);
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

    public List<OntologyTerm> getTerms() {
        return unmodifiableList(terms);
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
                return term.getAccession();
            }
        }));
    }

    @Override
    public String toString() {
        return "AssayProperty{" +
                "propertyValue=" + propertyValue +
                ", terms='" + terms + '\'' +
                '}';
    }
}

/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.microarray.atlas.model.bioentity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

/**
 * User: nsklyar
 * Date: 24/05/2011
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class BioEntityProperty {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bePropSeq")
    @SequenceGenerator(name = "bePropSeq", sequenceName = "A2_BIOENTITYPROPERTY_SEQ", allocationSize = 1)
    private Long bioEntitypropertyId;
    private String name;

    BioEntityProperty() {
    }

    public BioEntityProperty(Long bioEntitypropertyId, String name) {
        this.bioEntitypropertyId = bioEntitypropertyId;
        this.name = name;
    }

    public Long getBioEntitypropertyId() {
        return bioEntitypropertyId;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioEntityProperty that = (BioEntityProperty) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}

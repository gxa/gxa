package uk.ac.ebi.microarray.atlas.model.bioentity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

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

package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable
public final class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "propertySeq")
    @SequenceGenerator(name = "propertySeq", sequenceName = "A2_PROPERTY_SEQ")
    private Long propertyid;
    private String name;
    @OneToMany(targetEntity = PropertyValue.class, mappedBy = "property", orphanRemoval = true)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private List<PropertyValue> values = new ArrayList<PropertyValue>();

    Property() {
    }

    public Property(Long id, String name) {
        this.propertyid = id;
        this.name = name;
    }

    public Long getId() {
        return propertyid;
    }

    public String getName() {
        return name;
    }

    public List<PropertyValue> getValues() {
        return Collections.unmodifiableList(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Property that = (Property) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Property{" +
                "id=" + propertyid +
                ", name='" + name + '\'' +
                '}';
    }
}

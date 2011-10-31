package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

import javax.annotation.Nonnull;
import javax.persistence.*;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@Immutable
public final class PropertyValue implements Comparable<PropertyValue> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "propertyValueSeq")
    @SequenceGenerator(name = "propertyValueSeq", sequenceName = "A2_PROPERTYVALUE_SEQ", allocationSize = 1)
    private Long propertyvalueid;
    @Nonnull
    @ManyToOne
    private Property property;
    @Nonnull
    @Column(name = "NAME")
    private String value;
    private String displayName;

    PropertyValue() {
    }

    public PropertyValue(Long id, Property definition, String value) {
        if (definition == null)
            throw new NullPointerException("Definition must be provided");
        if (value == null)
            throw new NullPointerException("Value must be provided");

        this.propertyvalueid = id;
        this.property = definition;
        this.value = value;
    }

    public Long getId() {
        return propertyvalueid;
    }

    @Nonnull
    public Property getDefinition() {
        return property;
    }

    @Nonnull
    public String getValue() {
        return value;
    }

    public String getDisplayValue() {
        return displayName == null ? value : displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyValue that = (PropertyValue) o;

        return property.equals(that.property) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return property.hashCode() * 31 + value.hashCode();
    }

    @Override
    public String toString() {
        return "PropertyValue{" +
                "id=" + propertyvalueid +
                ", definition=" + property +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public int compareTo(PropertyValue o) {
        int result = property.compareTo(o.property);
        return result == 0 ? value.compareTo(o.value) : result;
    }
}

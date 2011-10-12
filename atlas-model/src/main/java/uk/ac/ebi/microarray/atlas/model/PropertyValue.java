package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import uk.ac.ebi.gxa.utils.StringUtil;

import javax.persistence.*;

@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Immutable
public final class PropertyValue {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "propertyValueSeq")
    @SequenceGenerator(name = "propertyValueSeq", sequenceName = "A2_PROPERTYVALUE_SEQ", allocationSize = 1)
    private Long propertyvalueid;
    @ManyToOne
    private Property property;
    @Column(name = "NAME")
    private String value;
    private String displayName;

    PropertyValue() {
    }

    public PropertyValue(Long id, Property definition, String value) {
        this.propertyvalueid = id;
        this.property = definition;
        this.value = value;
    }

    public Long getId() {
        return propertyvalueid;
    }

    public Property getDefinition() {
        return property;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayValue() {
        return displayName == null ? StringUtil.prettify(value) : displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyValue that = (PropertyValue) o;

        if (property != null ? !property.equals(that.property) : that.property != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = property != null ? property.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        return "PropertyValue{" +
                "id=" + propertyvalueid +
                ", definition=" + property +
                ", value='" + value + '\'' +
                '}';
    }
}

package uk.ac.ebi.microarray.atlas.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public final class PropertyValue {
    @Id
    private Long propertyvalueid;
    @ManyToOne
    private Property property;
    @Column(name = "NAME")
    private String value;

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

    public PropertyValue withId(Long id) {
        return new PropertyValue(id, property, value);
    }

    public PropertyValue withDefinition(Property definition) {
        return new PropertyValue(propertyvalueid, definition, value);
    }

    public PropertyValue withValue(String value) {
        return new PropertyValue(propertyvalueid, property, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyValue that = (PropertyValue) o;

        if (property != null ? !property.equals(that.property) : that.property != null) return false;
        if (propertyvalueid != null ? !propertyvalueid.equals(that.propertyvalueid) : that.propertyvalueid != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = propertyvalueid != null ? propertyvalueid.hashCode() : 0;
        result = 31 * result + (property != null ? property.hashCode() : 0);
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

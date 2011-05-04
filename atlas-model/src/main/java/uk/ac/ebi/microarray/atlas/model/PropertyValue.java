package uk.ac.ebi.microarray.atlas.model;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class PropertyValue {
    private final Long id;
    private final PropertyDefinition definition;
    private final String value;

    public PropertyValue(Long id, PropertyDefinition definition, String value) {
        this.id = id;
        this.definition = definition;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public PropertyDefinition getDefinition() {
        return definition;
    }

    public String getValue() {
        return value;
    }

    public PropertyValue withId(Long id) {
        return new PropertyValue(id, definition, value);
    }

    public PropertyValue withDefinition(PropertyDefinition definition) {
        return new PropertyValue(id, definition, value);
    }

    public PropertyValue withValue(String value) {
        return new PropertyValue(id, definition, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyValue that = (PropertyValue) o;

        if (definition != null ? !definition.equals(that.definition) : that.definition != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (definition != null ? definition.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PropertyValue{" +
                "id=" + id +
                ", definition=" + definition +
                ", value='" + value + '\'' +
                '}';
    }
}

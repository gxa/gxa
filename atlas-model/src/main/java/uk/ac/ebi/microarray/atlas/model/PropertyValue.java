package uk.ac.ebi.microarray.atlas.model;

import javax.annotation.concurrent.Immutable;

@Immutable
public class PropertyValue {
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
}

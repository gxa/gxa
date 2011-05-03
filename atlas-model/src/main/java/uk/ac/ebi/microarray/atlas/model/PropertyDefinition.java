package uk.ac.ebi.microarray.atlas.model;

import javax.annotation.concurrent.Immutable;

@Immutable
public class PropertyDefinition {
    private final Long id;
    private final String name;

    public PropertyDefinition(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PropertyDefinition withName(String name) {
        return new PropertyDefinition(id, name);
    }

    public PropertyDefinition withId(Long id) {
        return new PropertyDefinition(id, name);
    }
}

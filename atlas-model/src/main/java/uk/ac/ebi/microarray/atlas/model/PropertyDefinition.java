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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyDefinition that = (PropertyDefinition) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}

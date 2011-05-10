package uk.ac.ebi.microarray.atlas.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public final class Property {
    @Id
    private Long propertyid;
    private String name;

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

    public Property withName(String name) {
        return new Property(propertyid, name);
    }

    public Property withId(Long id) {
        return new Property(id, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Property that = (Property) o;

        if (propertyid != null ? !propertyid.equals(that.propertyid) : that.propertyid != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = propertyid != null ? propertyid.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Property{" +
                "id=" + propertyid +
                ", name='" + name + '\'' +
                '}';
    }
}

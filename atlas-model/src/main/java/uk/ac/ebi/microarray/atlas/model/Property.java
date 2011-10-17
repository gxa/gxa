package uk.ac.ebi.microarray.atlas.model;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import uk.ac.ebi.gxa.utils.StringUtil;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public final class Property implements Comparable<Property> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "propertySeq")
    @SequenceGenerator(name = "propertySeq", sequenceName = "A2_PROPERTY_SEQ", allocationSize = 1)
    private Long propertyid;
    private String name;
    private String displayName;
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

    public String getDisplayName() {
        return displayName == null ? StringUtil.prettify(name) : displayName;
    }

    public List<PropertyValue> getValues() {
        return unmodifiableList(values);
    }

    public void deleteValue(PropertyValue propertyValue) {
        values.remove(propertyValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Property that = (Property) o;

        return name == null ? that.name == null : name.equals(that.name);
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

    @Override
    public int compareTo(Property o) {
        return name.compareTo(o.name);
    }
}

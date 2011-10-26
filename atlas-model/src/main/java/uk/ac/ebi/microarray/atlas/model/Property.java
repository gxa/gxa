package uk.ac.ebi.microarray.atlas.model;

import org.apache.commons.lang.IncompleteArgumentException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.gxa.utils.StringUtil;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public final class Property implements Comparable<Property> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "propertySeq")
    @SequenceGenerator(name = "propertySeq", sequenceName = "A2_PROPERTY_SEQ", allocationSize = 1)
    private Long propertyid;
    private String name;
    private String displayName;
    @OneToMany(targetEntity = PropertyValue.class, mappedBy = "property", orphanRemoval = true)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    private List<PropertyValue> values = new ArrayList<PropertyValue>();

    Property() {
    }

    private Property(Long id, String accession, String displayName) {
        this.propertyid = id;
        this.name = accession;
        this.displayName = displayName;
    }

    public static String getSanitizedPropertyAccession(String name) {
        return EscapeUtil.encode(name).toLowerCase();
    }

    public static Property createProperty(String displayName) {
        return createProperty(null, getSanitizedPropertyAccession(displayName), displayName);
    }

    public static Property createProperty(Long id, String accession, String displayName) {
        if (!accession.equals(getSanitizedPropertyAccession(accession)))
            throw new IncompleteArgumentException("Property accession must be sanitized");

        return new Property(id, accession, displayName);
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

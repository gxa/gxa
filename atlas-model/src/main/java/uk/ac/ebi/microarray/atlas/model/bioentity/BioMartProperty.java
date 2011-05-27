package uk.ac.ebi.microarray.atlas.model.bioentity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

/**
 * User: nsklyar
 * Date: 23/05/2011
 */
@Entity
public class BioMartProperty {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bmPropSeq")
    @SequenceGenerator(name = "bmPropSeq", sequenceName = "A2_BIOMARTPROPERTY_SEQ")
    private Long biomartpropertyId;
    private String biomartPropertyName;

    @ManyToOne
    private BEProperty property;

    public BioMartProperty(Long id, String biomartPropertyName, BEProperty property) {
        this.biomartpropertyId = id;
        this.biomartPropertyName = biomartPropertyName;
        this.property = property;
    }

    public Long getId() {
        return biomartpropertyId;
    }

    public String getBiomartPropertyName() {
        return biomartPropertyName;
    }

    public BEProperty getProperty() {
        return property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioMartProperty that = (BioMartProperty) o;

        if (biomartPropertyName != null ? !biomartPropertyName.equals(that.biomartPropertyName) : that.biomartPropertyName != null)
            return false;
        if (property != null ? !property.equals(that.property) : that.property != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = biomartPropertyName != null ? biomartPropertyName.hashCode() : 0;
        result = 31 * result + (property != null ? property.hashCode() : 0);
        return result;
    }
}

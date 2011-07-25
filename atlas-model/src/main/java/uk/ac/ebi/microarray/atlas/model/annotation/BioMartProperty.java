package uk.ac.ebi.microarray.atlas.model.annotation;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
    private String name;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private BioEntityProperty bioEntityProperty;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private BioMartAnnotationSource annotationSrc;

    BioMartProperty() {
    }

    BioMartProperty(Long id, String biomartPropertyName, BioEntityProperty bioEntityProperty) {
        this.biomartpropertyId = id;
        this.name = biomartPropertyName;
        this.bioEntityProperty = bioEntityProperty;
    }

    public BioMartProperty(String biomartPropertyName, BioEntityProperty bioEntityProperty, BioMartAnnotationSource annSrc) {
        this.name = biomartPropertyName;
        this.bioEntityProperty = bioEntityProperty;
        this.annotationSrc = annSrc;
    }

    public Long getId() {
        return biomartpropertyId;
    }

    public String getName() {
        return name;
    }

    public BioEntityProperty getBioEntityProperty() {
        return bioEntityProperty;
    }

    public Long getBiomartpropertyId() {
        return biomartpropertyId;
    }

    void setBiomartpropertyId(Long biomartpropertyId) {
        this.biomartpropertyId = biomartpropertyId;
    }

    public BioMartAnnotationSource getAnnotationSrc() {
        return annotationSrc;
    }

     void setAnnotationSrc(BioMartAnnotationSource annotationSrc) {
        this.annotationSrc = annotationSrc;
    }

    void setBioEntityProperty(BioEntityProperty bioEntityProperty) {
        this.bioEntityProperty = bioEntityProperty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioMartProperty that = (BioMartProperty) o;

        if (annotationSrc != null ? !annotationSrc.equals(that.annotationSrc) : that.annotationSrc != null)
            return false;
        if (bioEntityProperty != null ? !bioEntityProperty.equals(that.bioEntityProperty) : that.bioEntityProperty != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (bioEntityProperty != null ? bioEntityProperty.hashCode() : 0);
        result = 31 * result + (annotationSrc != null ? annotationSrc.hashCode() : 0);
        return result;
    }
}

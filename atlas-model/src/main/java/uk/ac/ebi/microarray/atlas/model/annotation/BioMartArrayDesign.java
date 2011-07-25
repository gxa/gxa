package uk.ac.ebi.microarray.atlas.model.annotation;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

/**
 * User: nsklyar
 * Date: 19/07/2011
 */
@Entity
public class BioMartArrayDesign {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bmPropSeq")
    @SequenceGenerator(name = "bmPropSeq", sequenceName = "A2_BIOMARTARRAYDESIGN_SEQ")
    private Long biomartarraydesignId;
    private String name;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private ArrayDesign arrayDesign;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private BioMartAnnotationSource annotationSrc;

    BioMartArrayDesign() {
    }

    public BioMartArrayDesign(Long biomartarraydesignId, String name, ArrayDesign arrayDesign, BioMartAnnotationSource annSrc) {
        this.biomartarraydesignId = biomartarraydesignId;
        this.name = name;
        this.arrayDesign = arrayDesign;
        this.annotationSrc = annSrc;
    }

    public Long getBiomartarraydesignId() {
        return biomartarraydesignId;
    }

    public void setBiomartarraydesignId(Long biomartarraydesignId) {
        this.biomartarraydesignId = biomartarraydesignId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    public void setArrayDesign(ArrayDesign arrayDesign) {
        this.arrayDesign = arrayDesign;
    }

    public BioMartAnnotationSource getAnnotationSrc() {
        return annotationSrc;
    }

    public void setAnnotationSrc(BioMartAnnotationSource annotationSrc) {
        this.annotationSrc = annotationSrc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BioMartArrayDesign that = (BioMartArrayDesign) o;

        if (annotationSrc != null ? !annotationSrc.equals(that.annotationSrc) : that.annotationSrc != null)
            return false;
        if (arrayDesign != null ? !arrayDesign.equals(that.arrayDesign) : that.arrayDesign != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (arrayDesign != null ? arrayDesign.hashCode() : 0);
        result = 31 * result + (annotationSrc != null ? annotationSrc.hashCode() : 0);
        return result;
    }
}

package uk.ac.ebi.microarray.atlas.model.bioentity;

import uk.ac.ebi.microarray.atlas.model.Organism;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: nsklyar
 * Date: 16/05/2011
 */
@Entity
@Table(name="A2_CURRENTANNOTATIONSRC")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "annsrctype",
        discriminatorType = DiscriminatorType.STRING
)
public abstract class CurrentAnnotationSource<T extends AnnotationSource> {

    @Id
    protected Long currentAnnotationSrcId;

    @Transient
    protected T source;

    @ManyToOne
    protected BioEntityType bioentityType;

    @Temporal(TemporalType.DATE)
    protected Date loaddate;

    protected CurrentAnnotationSource() {
    }

    protected CurrentAnnotationSource(T source, BioEntityType bioentityType) {
        if (!source.getTypes().contains(bioentityType)) {
            throw new IllegalArgumentException("Annotation Source " + source.getDisplayName() + "doesn't apply to type " + bioentityType);
        }
        this.source = source;
        this.bioentityType = bioentityType;
        this.loaddate = getLoaddate();
    }

    protected CurrentAnnotationSource(Long currentAnnotationSrcId, T source, BioEntityType bioentityType) {
        this(source, bioentityType);
        this.currentAnnotationSrcId = currentAnnotationSrcId;
    }

    public Long getCurrentAnnotationSrcId() {
        return currentAnnotationSrcId;
    }

    @ManyToOne
    public Organism getOrganism() {
        return source.getOrganism();
    }

    public T getSource() {
        return source;
    }

    public BioEntityType getBioentityType() {
        return bioentityType;
    }

    public Date getLoaddate() {
        return loaddate;
    }

    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        return dateFormat.format(date);
    }
}

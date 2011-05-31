package uk.ac.ebi.microarray.atlas.model.bioentity;

import uk.ac.ebi.microarray.atlas.model.Organism;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: nsklyar
 * Date: 16/05/2011
 */
@Entity
public class CurrentAnnotationSource<T extends AnnotationSource> {

    @ManyToOne
    private T source;

    @ManyToOne
    private BioEntityType type;

    @Temporal(TemporalType.DATE)
    private Date date;


    public CurrentAnnotationSource(T source, BioEntityType type) {
//        if (!source.getTypes().contains(type)) {
//            throw new IllegalArgumentException("Annotation Source " + source.getDisplayName() + "doesn't apply to type " + type);
//        }
        this.source = source;
        this.type = type;
        this.date = getDate();
    }

    public Organism getOrganism() {
        return source.getOrganism();
    }

    public T getSource() {
        return source;
    }

    public BioEntityType getType() {
        return type;
    }

    public Date getDate() {
        return date;
    }

    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        return dateFormat.format(date);
    }
}

package uk.ac.ebi.gxa.annotator.model;

import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static uk.ac.ebi.gxa.utils.DateUtil.copyOf;

/**
 * User: nsklyar
 * Date: 09/05/2011
 */
@Entity
@Table(name="A2_ANNOTATIONSRC")
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "annsrctype",
        discriminatorType = DiscriminatorType.STRING
)
public abstract class AnnotationSource implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annSrcSeq")
    @SequenceGenerator(name = "annSrcSeq", sequenceName = "A2_ANNOTATIONSRC_SEQ", allocationSize = 1)
    protected Long annotationSrcId;

    @ManyToOne()
    protected Organism organism;

    @ManyToOne()
    protected Software software;

    @ManyToMany (fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH,CascadeType.REFRESH })
    @JoinTable(name = "A2_ANNSRC_BIOENTITYTYPE",
            joinColumns = @JoinColumn(name = "annotationsrcid", referencedColumnName = "annotationsrcid"),
            inverseJoinColumns = @JoinColumn(name = "bioentitytypeid", referencedColumnName = "bioentitytypeid"))
    protected Set<BioEntityType> types = new HashSet<BioEntityType>();

    @Temporal(TemporalType.DATE)
    protected Date loadDate;
    @Transient
    private boolean isApplied = false;

    protected AnnotationSource() {
    }

    protected AnnotationSource(Long annotationSrcId, Software software, Organism organism) {
        this.annotationSrcId = annotationSrcId;
        this.organism = organism;
        this.software = software;
    }

    public AnnotationSource(Software software, Organism organism) {
        this.software = software;
        this.organism = organism;
    }

    public Long getAnnotationSrcId() {
        return annotationSrcId;
    }

    public Set<BioEntityType> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    public void addBioEntityType(BioEntityType type) {
        types.add(type);
    }

    public boolean removeBioEntityType(BioEntityType type) {
       return types.remove(type);
    }

    public Organism getOrganism() {
        return organism;
    }


    public Software getSoftware() {
        return software;
    }

    public Date getLoadDate() {
        return copyOf(loadDate);
    }

    public void setLoadDate(Date loadDate) {
        this.loadDate = copyOf(loadDate);
    }


    @Override
    public String toString() {
        return "AnnotationSource{" +
                "annotationSrcId=" + annotationSrcId +
                ", organism=" + organism +
                ", software=" + software +
                ", types=" + types +
                ", loadDate=" + loadDate +
                '}';
    }

    public boolean isApplied() {
        return isApplied;
    }

    public void setApplied(boolean applied) {
        isApplied = applied;
    }
}

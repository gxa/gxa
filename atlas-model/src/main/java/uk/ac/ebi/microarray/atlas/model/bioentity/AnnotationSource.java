package uk.ac.ebi.microarray.atlas.model.bioentity;

import uk.ac.ebi.microarray.atlas.model.Organism;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 09/05/2011
 */
@Entity
@Inheritance(strategy= InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "annsrctype",
        discriminatorType = DiscriminatorType.STRING
)
public abstract class AnnotationSource{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annSrcSeq")
    @SequenceGenerator(name = "annSrcSeq", sequenceName = "A2_ANNOTATIONSRC_SEQ")
    protected Long annotationSrcId;
    @ManyToOne
    protected Organism organism;

    @ManyToOne(cascade = CascadeType.PERSIST)
    protected Software software;
    @ManyToMany
    @JoinTable(name = "A2_ANNSRCBETYPE",
            joinColumns = @JoinColumn(name = "annoationsrcid", referencedColumnName = "annoationsrcid"),
            inverseJoinColumns = @JoinColumn(name = "bioentitytypeid", referencedColumnName = "bioentitytypeid"))
    protected Set<BioEntityType> types = new HashSet<BioEntityType>();

    public AnnotationSource(Software software, Organism organism) {
        this.software = software;
        this.organism = organism;
    }

    public Long getAnnotationSrcId() {
        return annotationSrcId;
    }

    public void setAnnotationSrcId(Long annotationSrcId) {
        this.annotationSrcId = annotationSrcId;
    }

    public Collection<BioEntityType> getTypes() {
        return Collections.unmodifiableCollection(types);
    }

    public void addBioentityType(BioEntityType type) {
        types.add(type);
    }

    public Organism getOrganism() {
        return organism;
    }

    public void setOrganism(Organism organism) {
        this.organism = organism;
    }

    public Software getSoftware() {
        return software;
    }

    public String getDisplayName() {
        return software.getDisplayName() + (organism!=null?"-" + organism.getName():"");
    }

    public abstract boolean isUpdatable();


    public Collection<CurrentAnnotationSource<? extends AnnotationSource>> generateCurrentAnnSrcs() {
        List<CurrentAnnotationSource<? extends AnnotationSource>> result = new ArrayList<CurrentAnnotationSource<? extends AnnotationSource>>();
        for (BioEntityType bioEntityType : this.getTypes()) {
            result.add(this.createCurrAnnSrc(bioEntityType));
        }
        return result;
    }

    protected abstract CurrentAnnotationSource<? extends AnnotationSource> createCurrAnnSrc(BioEntityType bioEntityType);
}

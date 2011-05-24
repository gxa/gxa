package uk.ac.ebi.microarray.atlas.model.bioentity;

import uk.ac.ebi.microarray.atlas.model.Organism;

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
public abstract class AnnotationSource extends Software{
    protected Long annotationSrcId;
    protected Organism organism;
    protected Set<BioEntityType> types = new HashSet<BioEntityType>();

    public AnnotationSource(String name, String version, Organism organism) {
        super(name, version);
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

    public String getDisplayName() {
        return super.getDisplayName() + (organism!=null?"-" + organism.getName():"");
    }

    public abstract boolean isUpdatable();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AnnotationSource that = (AnnotationSource) o;

        if (organism != null ? !organism.equals(that.organism) : that.organism != null) return false;
        if (types != null ? !types.equals(that.types) : that.types != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (organism != null ? organism.hashCode() : 0);
        result = 31 * result + (types != null ? types.hashCode() : 0);
        return result;
    }

    public Collection<CurrentAnnotationSource<? extends AnnotationSource>> generateCurrentAnnSrcs() {
        List<CurrentAnnotationSource<? extends AnnotationSource>> result = new ArrayList<CurrentAnnotationSource<? extends AnnotationSource>>();
        for (BioEntityType bioEntityType : this.getTypes()) {
            result.add(this.createCurrAnnSrc(bioEntityType));
        }
        return result;
    }

    protected abstract CurrentAnnotationSource<? extends AnnotationSource> createCurrAnnSrc(BioEntityType bioEntityType);
}

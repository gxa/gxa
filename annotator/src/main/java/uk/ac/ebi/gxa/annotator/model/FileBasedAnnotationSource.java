package uk.ac.ebi.gxa.annotator.model;

import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

/**
 * User: nsklyar
 * Date: 04/01/2012
 */
@MappedSuperclass
public abstract class FileBasedAnnotationSource extends AnnotationSource {
    protected FileBasedAnnotationSource() {
    }

    protected FileBasedAnnotationSource(Software software) {
        super(software);
        this.name = createName();
    }

    @Override
    protected final String createName() {
        return getSoftware().getFullName();
    }

    public abstract char getSeparator();
}

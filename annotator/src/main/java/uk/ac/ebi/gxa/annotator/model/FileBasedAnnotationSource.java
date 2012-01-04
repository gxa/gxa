package uk.ac.ebi.gxa.annotator.model;

import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

/**
 * User: nsklyar
 * Date: 04/01/2012
 */
@Entity
@MappedSuperclass
public abstract class FileBasedAnnotationSource extends AnnotationSource {
    protected FileBasedAnnotationSource() {
    }

    protected FileBasedAnnotationSource(Software software) {
        super(software);
    }

    @Override
    protected String createName() {
        return getSoftware().getFullName();
    }

    public abstract char getSeparator();
}

package uk.ac.ebi.microarray.atlas.model.bioentity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * User: nsklyar
 * Date: 02/06/2011
 */
@Entity
@DiscriminatorValue("file")
public class FileCurrentAnnotationSource extends CurrentAnnotationSource<FileAnnotationSource>{

    FileCurrentAnnotationSource(FileAnnotationSource source, BioEntityType type) {
        super(source, type);
    }

    @Override
    @ManyToOne
    public FileAnnotationSource getSource() {
        return super.getSource();
    }
}

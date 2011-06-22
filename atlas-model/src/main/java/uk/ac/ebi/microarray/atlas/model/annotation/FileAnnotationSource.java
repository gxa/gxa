package uk.ac.ebi.microarray.atlas.model.annotation;

import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * User: nsklyar
 * Date: 10/05/2011
 */
@Entity
@DiscriminatorValue("file")
public class FileAnnotationSource extends AnnotationSource{

    @Column(name = "url")
    private String fileName;

    FileAnnotationSource() {
    }

    public FileAnnotationSource(Software software, Organism organism, String fileName) {
        super(software, organism);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean isUpdatable() {
        return false;  
    }

}

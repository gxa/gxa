package uk.ac.ebi.gxa.annotator.model;

import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("reactome")
public class ReactomeAnnotationSource extends FileBasedAnnotationSource {
    ReactomeAnnotationSource() {
    }

    public ReactomeAnnotationSource(Software software) {
        super(software);
    }

    @Override
    public char getSeparator() {
        return '\t';
    }
}

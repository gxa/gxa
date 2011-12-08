package uk.ac.ebi.gxa.annotator.model.genesigdb;

import uk.ac.ebi.gxa.annotator.loader.filebased.FileBasedConnection;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Collection;
import java.util.HashSet;

/**
 * User: nsklyar
 * Date: 19/10/2011
 */
@Entity
//@Table(name = "A2_GENESIGANNOTATIONSRC")
@DiscriminatorValue("genesigdb")
public class GeneSigAnnotationSource extends AnnotationSource {

    GeneSigAnnotationSource() {
    }

    public GeneSigAnnotationSource(Software software) {
        super(software);
    }

    @Override
    protected String createName() {
        return software.getFullName();
    }

    @Override
    public GeneSigAnnotationSource createCopyForNewSoftware(Software newSoftware) {
        GeneSigAnnotationSource result = new GeneSigAnnotationSource(newSoftware);
        result.setUrl(this.url);

        return result;
    }

    @Override
    public FileBasedConnection createConnection() {
        return new FileBasedConnection(this.getUrl());
    }

    @Override
    public Collection<String> findInvalidProperties() {
        Collection<String> result = new HashSet<String>();
        return result;
    }

    public char getSeparator() {
        return ',';
    }
}

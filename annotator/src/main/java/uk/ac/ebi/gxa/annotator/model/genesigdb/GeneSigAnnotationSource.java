package uk.ac.ebi.gxa.annotator.model.genesigdb;

import uk.ac.ebi.gxa.annotator.loader.filebased.GeneSigConnection;
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
@DiscriminatorValue("genesigdb")
public class GeneSigAnnotationSource extends AnnotationSource {

    GeneSigAnnotationSource() {
    }

    public GeneSigAnnotationSource(Software software) {
        super(software);
    }

    @Override
    protected String createName() {
        return getSoftware().getFullName();
    }

    public GeneSigAnnotationSource createCopyForNewSoftware(Software newSoftware) {
        GeneSigAnnotationSource result = new GeneSigAnnotationSource(newSoftware);
        updateProperties(result);
        return result;
    }

    @Override
    public GeneSigConnection createConnection() {
        return new GeneSigConnection(this.getUrl());
    }

    @Override
    public Collection<String> findInvalidProperties() {
        Collection<String> result = new HashSet<String>();
        final GeneSigConnection connection = createConnection();
//        connection.validateAttributeNames(getExternalPropertyNames());
        return result;
    }

    public char getSeparator() {
        return ',';
    }
}

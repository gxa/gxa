package uk.ac.ebi.gxa.annotator.model;

import uk.ac.ebi.gxa.annotator.model.connection.AnnotationSourceAccessException;
import uk.ac.ebi.gxa.annotator.model.connection.GeneSigConnection;
import uk.ac.ebi.gxa.exceptions.LogUtil;
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
public class GeneSigAnnotationSource extends FileBasedAnnotationSource {

    GeneSigAnnotationSource() {
    }

    public GeneSigAnnotationSource(Software software) {
        super(software);
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
        try {
            final GeneSigConnection connection = createConnection();
            connection.validateAttributeNames(getExternalPropertyNames());
            return result;
        } catch (AnnotationSourceAccessException e) {
            throw LogUtil.createUnexpected("Problem when fetching version for " + this.getSoftware().getName(), e);
        }
    }

    public char getSeparator() {
        return ',';
    }
}

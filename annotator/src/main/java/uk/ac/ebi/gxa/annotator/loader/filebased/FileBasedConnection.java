package uk.ac.ebi.gxa.annotator.loader.filebased;

import uk.ac.ebi.gxa.annotator.loader.AnnotationSourceConnection;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAccessException;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;

import java.util.Collection;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 26/10/2011
 */
public class FileBasedConnection<T extends AnnotationSource> extends AnnotationSourceConnection<T> {
    public FileBasedConnection(String url) {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public String getOnlineMartVersion() throws BioMartAccessException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<String> validateAttributeNames(Set<String> properties) throws BioMartAccessException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

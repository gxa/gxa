package uk.ac.ebi.gxa.annotator.loader.filebased;

import uk.ac.ebi.gxa.annotator.loader.AnnotationSourceConnection;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAccessException;
import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 26/10/2011
 */
public class FileBasedConnection extends AnnotationSourceConnection<GeneSigAnnotationSource> {
    public FileBasedConnection(String url) {

    }

    @Override
    public String getOnlineMartVersion() throws BioMartAccessException {
        return "4";
    }

    @Override
    public Collection<String> validateAttributeNames(Set<String> properties) throws BioMartAccessException {
        return null;  
    }

    public URL getURL(String location) throws IOException {
        return new URL(location);
    }
}

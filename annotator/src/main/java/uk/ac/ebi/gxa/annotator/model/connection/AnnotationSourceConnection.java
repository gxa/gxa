package uk.ac.ebi.gxa.annotator.model.connection;

import uk.ac.ebi.gxa.annotator.model.AnnotationSource;

import java.util.Collection;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 26/10/2011
 */
public interface AnnotationSourceConnection <T extends AnnotationSource> {

    public abstract String getOnlineSoftwareVersion() throws AnnotationSourceAccessException;

    public abstract Collection<String> validateAttributeNames(Set<String> properties) throws AnnotationSourceAccessException;
}

package uk.ac.ebi.gxa.annotator.loader;

import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAccessException;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;

import java.util.Collection;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 26/10/2011
 */
public abstract class AnnotationSourceConnection <T extends AnnotationSource> {


    public abstract String getOnlineMartVersion() throws BioMartAccessException;

    public abstract Collection<String> validateAttributeNames(Set<String> properties) throws BioMartAccessException;
}

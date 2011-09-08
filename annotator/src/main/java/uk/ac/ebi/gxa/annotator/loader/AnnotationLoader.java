package uk.ac.ebi.gxa.annotator.loader;

import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public interface AnnotationLoader {

 public void annotate(AnnotationCommand cmd, AnnotationLoaderListener listener);
}

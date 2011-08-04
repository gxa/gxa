package uk.ac.ebi.gxa.annotator.loader;

import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public abstract class AnnotationCommand {

    protected AnnotatorFactory factory;

    public void setAnnotatorFactory(AnnotatorFactory factory) {
        this.factory = factory;
    }

    public abstract void execute(AnnotationLoaderListener listener) throws AtlasAnnotationException;
}

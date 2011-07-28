package uk.ac.ebi.gxa.annotator.loader;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class DefaultAnnotationLoader implements AnnotationLoader {

    private AnnotatorFactory annotatorFactory;

    public DefaultAnnotationLoader(AnnotatorFactory annotatorFactory) {
        this.annotatorFactory = annotatorFactory;
    }

    @Override
    public void annotate(AnnotationCommand annotationCommand) {
        annotationCommand.setAnnotatorFactory(annotatorFactory);
        annotationCommand.execute();
    }
}

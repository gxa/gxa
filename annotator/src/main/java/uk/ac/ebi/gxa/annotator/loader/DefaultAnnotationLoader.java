package uk.ac.ebi.gxa.annotator.loader;


import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class DefaultAnnotationLoader implements AnnotationLoader {

    private AnnotatorFactory annotatorFactory;
    private ExecutorService executor;

    public DefaultAnnotationLoader(AnnotatorFactory annotatorFactory, ExecutorService executor) {
        this.annotatorFactory = annotatorFactory;
        this.executor = executor;
    }

    @Override
    public void annotate(final AnnotationCommand annotationCommand, final AnnotationLoaderListener listener) {
        annotationCommand.setAnnotatorFactory(annotatorFactory);

        executor.submit(new Callable<Boolean>() {
            public Boolean call() {
                annotationCommand.execute(listener);
                return true;
            }
        });


    }
}

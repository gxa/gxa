package uk.ac.ebi.gxa.annotator.loader;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderEvent;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class DefaultAnnotationLoader implements AnnotationLoader {

    private AnnotatorFactory annotatorFactory;
    private ExecutorService executor;

    // logging
    private final Logger log = LoggerFactory.getLogger(DefaultAnnotationLoader.class);

    public DefaultAnnotationLoader(AnnotatorFactory annotatorFactory, ExecutorService executor) {
        this.annotatorFactory = annotatorFactory;
        this.executor = executor;
    }

    @Override
    public void annotate(final AnnotationCommand annotationCommand, final AnnotationLoaderListener listener) {
        annotationCommand.setAnnotatorFactory(annotatorFactory);


        final Future<Boolean> task = executor.submit(new Callable<Boolean>() {
            public Boolean call() throws AtlasAnnotationException {
                annotationCommand.execute(listener);
                return true;
            }
        });

        if (listener != null) {
            new Thread(new Runnable() {
                public void run() {
                    boolean success = true;
                    Throwable observedError = null;

                    try {
                        task.get();
                    } catch (InterruptedException e) {
                        log.error("Interrupted", e);
                    } catch (ExecutionException e) {
                        observedError = e.getCause() != null ? e.getCause() : e;
                        success = false;
                    } catch (Throwable e) {
                        observedError = e;
                        success = false;
                    }
                    // create our completion event
                    if (success) {
                        listener.buildSuccess();
                    } else {
                        listener.buildError(new AnnotationLoaderEvent(Arrays.asList(observedError)));
                    }
                }
            }).start();
        }

    }
}

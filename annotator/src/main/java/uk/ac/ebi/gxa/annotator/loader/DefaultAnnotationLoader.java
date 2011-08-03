package uk.ac.ebi.gxa.annotator.loader;


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

    public DefaultAnnotationLoader(AnnotatorFactory annotatorFactory, ExecutorService executor) {
        this.annotatorFactory = annotatorFactory;
        this.executor = executor;
    }

    @Override
    public void annotate(final AnnotationCommand annotationCommand) {
        annotationCommand.setAnnotatorFactory(annotatorFactory);


        final Future<Boolean> task = executor.submit(new Callable<Boolean>() {
            public Boolean call() {
                annotationCommand.execute();
                return true;
            }
        });

        new Thread(new Runnable() {
                public void run() {
                    try {
                        task.get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (ExecutionException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }).start();

    }
}

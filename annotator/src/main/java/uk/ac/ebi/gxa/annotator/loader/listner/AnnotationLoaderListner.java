package uk.ac.ebi.gxa.annotator.loader.listner;

import java.util.EventListener;

/**
 * User: nsklyar
 * Date: 01/08/2011
 */
public interface AnnotationLoaderListner  extends EventListener {
    /**
     * Indicates that building or updating of an index completed successfully
     *
     */
    void buildSuccess();

    /**
     * Indicates that building or updating of an index exited with an error
     *
     * @param event the event representing this build failure
     */
    void buildError(AnnotationLoaderEvent event);

    /**
     * Is called by builder to provide some progress status report
     *
     * @param progressStatus a text string representing human-readable status line of current index builder process
     */
    void buildProgress(String progressStatus);
}

package uk.ac.ebi.gxa.index.builder.listener;

import java.util.EventListener;

/**
 * A Listener that can be used to determine when an IndexBuilder has completed
 * it's execution.
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public interface IndexBuilderListener extends EventListener {
    /**
     * Indicates that building or updating of an index completed successfully
     *
     * @param event the event representing this build success event
     */
    void buildSuccess(IndexBuilderEvent event);

    /**
     * Indicates that building or updating of an index exited with an error
     *
     * @param event the event representing this build failure
     */
    void buildError(IndexBuilderEvent event);

    /**
     * Is called by builder to provide some progress status report
     *
     * @param progressStatus a text string representing human-readable status line of current index builder process
     */
    void buildProgress(String progressStatus);

}

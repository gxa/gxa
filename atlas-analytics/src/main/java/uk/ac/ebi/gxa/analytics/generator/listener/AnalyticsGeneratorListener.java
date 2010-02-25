package uk.ac.ebi.gxa.analytics.generator.listener;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public interface AnalyticsGeneratorListener {
    /**
     * Indicates that building or updating of a set of NetCDFs completed
     * successfully
     *
     * @param event the event representing this build success event
     */
    void buildSuccess(AnalyticsGenerationEvent event);

    /**
     * Indicates that building or updating of a set of Analyticss exited with an
     * error
     *
     * @param event the event representing this build failure
     */
    void buildError(AnalyticsGenerationEvent event);

    /**
     * Is called by builder to provide some progress status report
     *
     * @param progressStatus a text string representing human-readable status line of current generator process
     */
    void buildProgress(String progressStatus);
}

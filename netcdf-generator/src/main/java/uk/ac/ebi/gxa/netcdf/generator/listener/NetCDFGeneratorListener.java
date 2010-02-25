package uk.ac.ebi.gxa.netcdf.generator.listener;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public interface NetCDFGeneratorListener {
    /**
     * Indicates that building or updating of a set of NetCDFs completed
     * successfully
     *
     * @param event the event representing this build success event
     */
    void buildSuccess(NetCDFGenerationEvent event);

    /**
     * Indicates that building or updating of a set of NetCDFs exited with an
     * error
     *
     * @param event the event representing this build failure
     */
    void buildError(NetCDFGenerationEvent event);

    /**
     * Is called by builder to provide some progress status report
     *
     * @param progressStatus a text string representing human-readable status line of current generator process
     */
    void buildProgress(String progressStatus);
}

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
  public void buildSuccess(AnalyticsGenerationEvent event);

  /**
   * Indicates that building or updating of a set of Analyticss exited with an
   * error
   *
   * @param event the event representing this build failure
   */
  public void buildError(AnalyticsGenerationEvent event);
}

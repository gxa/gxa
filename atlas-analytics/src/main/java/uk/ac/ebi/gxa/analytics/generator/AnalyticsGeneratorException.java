package uk.ac.ebi.gxa.analytics.generator;

/**
 * An exception that occurs whenever something went wrong during NetCDF
 * generation
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class AnalyticsGeneratorException extends Exception {
  public AnalyticsGeneratorException() {
    super();
  }

  public AnalyticsGeneratorException(String s) {
    super(s);
  }

  public AnalyticsGeneratorException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public AnalyticsGeneratorException(Throwable throwable) {
    super(throwable);
  }
}

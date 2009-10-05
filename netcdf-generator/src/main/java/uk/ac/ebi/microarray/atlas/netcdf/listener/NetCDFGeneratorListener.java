package uk.ac.ebi.microarray.atlas.netcdf.listener;

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
  public void buildSuccess(NetCDFGenerationEvent event);

  /**
   * Indicates that building or updating of a set of NetCDFs exited with an
   * error
   *
   * @param event the event representing this build failure
   */
  public void buildError(NetCDFGenerationEvent event);
}

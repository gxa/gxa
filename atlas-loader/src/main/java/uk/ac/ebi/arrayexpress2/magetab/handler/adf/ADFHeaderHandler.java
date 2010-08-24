package uk.ac.ebi.arrayexpress2.magetab.handler.adf;

/**
 * The interface for all handlers that can handle aspects of the header part of
 * the ADF.
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public interface ADFHeaderHandler extends ADFHandler {
  /**
   * Configure this handler with a String to read from.
   *
   * @param line the string to read from
   */
  void setData(String line);
}

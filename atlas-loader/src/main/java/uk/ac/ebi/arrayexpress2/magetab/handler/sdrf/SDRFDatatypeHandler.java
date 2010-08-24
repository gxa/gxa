package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf;

/**
 * An interface that designates a handler as a handler for datatype nodes in the
 * SDRF graph.  Using this interface, you can obtain the name of the datatype
 * this handler captures.
 *
 * @author Tony Burdett
 * @date 26-May-2009
 */
public interface SDRFDatatypeHandler extends SDRFHandler {
  /**
   * The name of the type of data, attached to the column in the SDRF.
   *
   * @return the datatype name
   */
  public String getDatatypeName();
}

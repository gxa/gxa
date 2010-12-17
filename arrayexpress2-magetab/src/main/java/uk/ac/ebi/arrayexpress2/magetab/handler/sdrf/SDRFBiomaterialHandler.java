package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf;

/**
 * An interface that designates a handler as a handler for biomaterial nodes in
 * the SDRF graph.  Using this interface, you can obtain the name of the
 * biomaterial this handler captures.
 *
 * @author Tony Burdett
 * @date 26-May-2009
 */
public interface SDRFBiomaterialHandler extends SDRFHandler {
  /**
   * The name of the type of biomaterial, attached to the column in the SDRF.
   *
   * @return the material name
   */
  public String getMaterialName();
}

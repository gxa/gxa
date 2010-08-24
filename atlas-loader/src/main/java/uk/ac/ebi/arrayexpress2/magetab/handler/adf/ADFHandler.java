package uk.ac.ebi.arrayexpress2.magetab.handler.adf;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign;
import uk.ac.ebi.arrayexpress2.magetab.handler.Handler;

/**
 * The interface for all handlers that can handle aspects of an ADF file.
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public interface ADFHandler extends Handler {
  /**
   * Configure this handler with a {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign}
   * to read from and write to.
   *
   * @param arrayDesign the array design to use as a data model
   */
  void setArrayDesign(MAGETABArrayDesign arrayDesign);
}

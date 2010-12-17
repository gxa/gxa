package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.handler.Handler;

/**
 * A handler for entries in an SDRF MAGE-TAB file.  This handler handles an
 * ordered array of Strings and creates relevant objects in the MAGE model.
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public interface SDRFHandler extends Handler {
  /**
   * The "name" describing the first node or attribute that this handler will
   * handle.  Once the handler has been initialized by calling {@link
   * #setData(String[], String[])}, this becomes the same as the values[0]
   * passed to this method.  Note that prior to this happening, this handler can
   * actually handle any node name.
   *
   * @return the tag for entries this handler will handle
   */
  String handlesName();

  /**
   * Configure this handler with a MAGETABInvestigation to read from and write
   * to.
   *
   * @param investigation the MAGETABInvestigation to utilise as a buffer
   */
  void setInvestigation(MAGETABInvestigation investigation);

  /**
   * Configure this handler with the headers and the values being read.  These
   * string arrays must be the same length, else a parse exception may occur
   * when reading.  If the ordering changes from the file, then incorrect data
   * may be stored.
   *
   * @param headers the headers to read from
   * @param values  the values to read
   */
  void setData(String[] headers, String[] values);

  /**
   * A special type of handle-type method that will cause this handler to return
   * the index of the last header it can handle in the current set of headers.
   * This can be used to determine the start and finish indices that a handler
   * can read from/to.
   *
   * @return the last column index that can be read
   */
  int assess();
}

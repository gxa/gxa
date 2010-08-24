package uk.ac.ebi.arrayexpress2.magetab.handler.idf;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.handler.Handler;

/**
 * A handler for entries in an IDF MAGE-TAB file.  This handler handles an
 * ordered array of Strings and creates relevant objects in the MAGE model.
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public interface IDFHandler extends Handler {
  /**
   * Configure this handler with a MAGETABInvestigation to read from and write
   * to.
   *
   * @param investigation the MAGETABInvestigation to utilise as a buffer
   */
  void setInvestigation(MAGETABInvestigation investigation);

  /**
   * Configure this handler with a String to read from.
   *
   * @param line the string to read from
   */
  void setData(String line);
}

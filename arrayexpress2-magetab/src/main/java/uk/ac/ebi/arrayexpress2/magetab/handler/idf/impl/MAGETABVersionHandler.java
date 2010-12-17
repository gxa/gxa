package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle MAGE-TAB Version fields in the
 * IDF.
 * <p/>
 * Tag: MAGE-TAB Version
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class MAGETABVersionHandler extends AbstractIDFHandler {
  public MAGETABVersionHandler() {
    setTag("mage-tabversion");
    setAllowedLength(1);
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.magetabVersion = value;
  }
}

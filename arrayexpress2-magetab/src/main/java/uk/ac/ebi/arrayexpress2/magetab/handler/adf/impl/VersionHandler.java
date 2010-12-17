package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles the version rows of this ADF.  Should be 1.0 or 1.1.
 * If absent, 1.0 is assumed.
 * <p/>
 * Tag: Version
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class VersionHandler extends AbstractADFHeaderHandler {
  public VersionHandler() {
    setTag("version");
    setAllowedLength(1);
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.version = value;
  }
}

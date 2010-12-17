package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles rows in the ADF that describe the arrayaexpress
 * accession number in a comment field.
 * <p/>
 * Tag: Comment[ArrayExpressAccession]<br/>
 *
 * @author Tony Burdett
 * @date 18-Feb-2010
 */
public class AccessionHandler extends AbstractADFHeaderHandler {
  public AccessionHandler() {
    setTag("comment[ArrayExpressAccession]");
    setAllowedLength(1);
  }

  public void readValue(String value) throws ParseException {
    // read
    arrayDesign.accession = value;
  }
}

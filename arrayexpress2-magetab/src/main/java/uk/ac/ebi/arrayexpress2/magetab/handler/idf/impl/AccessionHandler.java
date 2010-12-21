package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler that handles rows in the IDF that describe the arrayaexpress
 * accession number in a comment field.
 * <p/>
 * Tag: Comment[ArrayExpressAccession]
 *
 * @author Tony Burdett
 * @date 09-Mar-2009
 */
public class AccessionHandler extends AbstractIDFHandler {
  public AccessionHandler() {
    setTag("comment[ArrayExpressAccession]");
    setAllowedLength(1);
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.accession = value;
  }
}

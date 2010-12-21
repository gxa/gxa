package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler that handles the term source version declaration rows in IDF. These
 * elements are tied to a term source name.
 * <p/>
 * Tag: Term Source Version
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class TermSourceVersionHandler extends AbstractIDFHandler {
  public TermSourceVersionHandler() {
    setTag("termsourceversion");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.termSourceVersion.add(value);
  }
}

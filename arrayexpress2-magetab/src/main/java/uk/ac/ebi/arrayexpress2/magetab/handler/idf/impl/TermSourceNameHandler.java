package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler that handles the term source name declaration rows in IDF.  This
 * row declared names that are used within this document to refer to a
 * particular term source.
 * <p/>
 * Tag: Term Source Name
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class TermSourceNameHandler extends AbstractIDFHandler {
  public TermSourceNameHandler() {
    setTag("termsourcename");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.termSourceName.add(value);
  }
}

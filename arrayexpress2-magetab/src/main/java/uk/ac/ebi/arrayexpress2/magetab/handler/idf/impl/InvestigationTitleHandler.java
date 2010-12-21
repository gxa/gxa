package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Investigation Title fields in the
 * IDF.
 * <p/>
 * Tag: Investigation Title
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class InvestigationTitleHandler extends AbstractIDFHandler {
  public InvestigationTitleHandler() {
    setTag("investigationtitle");
    setAllowedLength(1);
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.investigationTitle = value;
  }
}

package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Publication Status fields in the
 * IDF.
 * <p/>
 * Tag: Publication Status
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PublicationStatusHandler extends AbstractIDFHandler {
  public PublicationStatusHandler() {
    setTag("publicationstatus");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.publicationStatus.add(value);
  }
}

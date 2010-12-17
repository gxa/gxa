package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Publication Author List fields in
 * the IDF.
 * <p/>
 * Tag: Publication Author List
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PublicationAuthorListHandler extends AbstractIDFHandler {
  public PublicationAuthorListHandler() {
    setTag("publicationauthorlist");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.publicationAuthorList.add(value);
  }
}

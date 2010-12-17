package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Publication Status Term Source Ref
 * fields in the IDF.
 * <p/>
 * Tag: Publication Status Term Source Ref
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PublicationStatusTermSourceRefHandler extends AbstractIDFHandler {
  public PublicationStatusTermSourceRefHandler() {
    setTag("publicationstatustermsourceref");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.publicationStatusTermSourceREF.add(value);
  }

}

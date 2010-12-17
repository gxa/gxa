package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Publication Status Term Accession
 * fields in the IDF.
 * <p/>
 * Tag: Publication Status Term Accession Number
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class PublicationStatusTermAccessionHandler extends AbstractIDFHandler {
  public PublicationStatusTermAccessionHandler() {
    setTag("publicationstatustermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.publicationStatusTermAccession.add(value);
  }
}

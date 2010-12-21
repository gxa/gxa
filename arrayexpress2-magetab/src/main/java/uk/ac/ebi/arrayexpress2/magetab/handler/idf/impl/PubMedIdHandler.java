package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle PubMed ID fields in the IDF.
 * <p/>
 * Tag: PubMed ID
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PubMedIdHandler extends AbstractIDFHandler {
  public PubMedIdHandler() {
    setTag("pubmedid");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.pubMedId.add(value);
  }
}

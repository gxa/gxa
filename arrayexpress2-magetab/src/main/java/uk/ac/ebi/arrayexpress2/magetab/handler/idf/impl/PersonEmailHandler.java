package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Person Email fields in the IDF.
 * <p/>
 * Tag: Person Email
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PersonEmailHandler extends AbstractIDFHandler {
  public PersonEmailHandler() {
    setTag("personemail");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.personEmail.add(value);
  }
}

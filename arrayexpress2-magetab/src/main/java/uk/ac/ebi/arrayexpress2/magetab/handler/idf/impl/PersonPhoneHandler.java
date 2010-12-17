package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Person Phone fields in the IDF.
 * <p/>
 * Tag: Person Phone
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PersonPhoneHandler extends AbstractIDFHandler {
  public PersonPhoneHandler() {
    setTag("personphone");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.personPhone.add(value);
  }
}

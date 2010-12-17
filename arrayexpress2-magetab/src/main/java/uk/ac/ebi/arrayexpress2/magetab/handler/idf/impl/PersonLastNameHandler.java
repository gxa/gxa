package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Person Last Name fields in the
 * IDF.
 * <p/>
 * Tag: Person Last Name
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PersonLastNameHandler extends AbstractIDFHandler {
  public PersonLastNameHandler() {
    setTag("personlastname");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.personLastName.add(value);
  }
}

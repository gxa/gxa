package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Person Mid Initial fields in the
 * IDF.
 * <p/>
 * Tag: Person Mid Initials
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PersonMidInitialsHandler extends AbstractIDFHandler {
  public PersonMidInitialsHandler() {
    setTag("personmidinitials");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.personMidInitials.add(value);
  }
}

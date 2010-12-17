package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Person Fax fields in the IDF.
 * <p/>
 * Tag: Person Fax
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PersonFaxHandler extends AbstractIDFHandler {
  public PersonFaxHandler() {
    setTag("personfax");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.personFax.add(value);
  }
}

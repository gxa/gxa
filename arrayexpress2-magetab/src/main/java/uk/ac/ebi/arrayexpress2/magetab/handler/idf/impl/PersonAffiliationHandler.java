package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Person Affiliation fields in the
 * IDF.
 * <p/>
 * Tag: Person Affiliation
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PersonAffiliationHandler extends AbstractIDFHandler {
  public PersonAffiliationHandler() {
    setTag("personaffiliation");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.personAffiliation.add(value);
  }
}

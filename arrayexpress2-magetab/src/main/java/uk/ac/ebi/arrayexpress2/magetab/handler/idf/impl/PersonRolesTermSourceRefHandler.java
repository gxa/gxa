package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Person Roles Term Source REF fields
 * in the IDF.
 * <p/>
 * Tag: Person Roles Term Source Ref
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PersonRolesTermSourceRefHandler extends AbstractIDFHandler {
  public PersonRolesTermSourceRefHandler() {
    setTag("personrolestermsourceref");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.personRolesTermSourceREF.add(value);
  }
}

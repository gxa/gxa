package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Person Roles Term Accession Number
 * fields in the IDF.
 * <p/>
 * Tag: Person Roles Term Accession Number
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class PersonRolesTermAccessionHandler extends AbstractIDFHandler {
  public PersonRolesTermAccessionHandler() {
    setTag("personrolestermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.personRolesTermAccession.add(value);
  }
}

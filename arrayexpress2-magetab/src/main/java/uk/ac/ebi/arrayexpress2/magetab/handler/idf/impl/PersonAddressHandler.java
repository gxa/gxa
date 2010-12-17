package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Person Address fields in the IDF.
 * <p/>
 * Tag: Person Address
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PersonAddressHandler extends AbstractIDFHandler {
  public PersonAddressHandler() {
    setTag("personaddress");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.personAddress.add(value);
  }
}

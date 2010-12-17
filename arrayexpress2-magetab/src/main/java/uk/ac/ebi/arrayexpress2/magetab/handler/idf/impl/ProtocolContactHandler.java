package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Protocol Contact fields in the
 * IDF.
 * <p/>
 * Tag: Protocol Contact
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ProtocolContactHandler extends AbstractIDFHandler {
  public ProtocolContactHandler() {
    setTag("protocolcontact");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.protocolContact.add(value);
  }
}

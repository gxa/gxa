package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Protocol Name fields in the IDF.
 * <p/>
 * Tag: Protocol Name
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ProtocolNameHandler extends AbstractIDFHandler {
  public ProtocolNameHandler() {
    setTag("protocolname");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.protocolName.add(value);
  }
}

package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Protocol Type fields in the IDF.
 * <p/>
 * Tag: Protocol Type
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ProtocolTypeHandler extends AbstractIDFHandler {
  public ProtocolTypeHandler() {
    setTag("protocoltype");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.protocolType.add(value);
  }
}

package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Protocol hardware fields in the
 * IDF.
 * <p/>
 * Tag: Protocol Hardware
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ProtocolHardwareHandler extends AbstractIDFHandler {
  public ProtocolHardwareHandler() {
    setTag("protocolhardware");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.protocolHardware.add(value);
  }
}

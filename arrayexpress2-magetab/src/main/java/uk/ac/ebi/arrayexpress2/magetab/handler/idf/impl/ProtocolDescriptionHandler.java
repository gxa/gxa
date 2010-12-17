package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Protocol Description fields in the
 * IDF.
 * <p/>
 * Tag: Protocol Description
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ProtocolDescriptionHandler extends AbstractIDFHandler {
  public ProtocolDescriptionHandler() {
    setTag("protocoldescription");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.protocolDescription.add(value);
  }
}

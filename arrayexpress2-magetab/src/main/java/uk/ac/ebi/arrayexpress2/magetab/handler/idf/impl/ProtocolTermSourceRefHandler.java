package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Protocol Term Source Ref fields in
 * the IDF.
 * <p/>
 * Tag: Protocol Term Source Ref
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ProtocolTermSourceRefHandler extends AbstractIDFHandler {
  public ProtocolTermSourceRefHandler() {
    setTag("protocoltermsourceref");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.protocolTermSourceREF.add(value);
  }
}

package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Protocol Term accession fields in
 * the IDF.
 * <p/>
 * Tag: Protocol Term Accession Number
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class ProtocolTermAccessionHandler extends AbstractIDFHandler {
  public ProtocolTermAccessionHandler() {
    setTag("protocoltermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.protocolTermAccession.add(value);
  }
}

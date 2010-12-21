package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handlers ADF rows describing the printing protocol
 * <p/>
 * Tag: Printing Protocol
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class PrintingProtocolHandler extends AbstractADFHeaderHandler {
  public PrintingProtocolHandler() {
    setTag("printingprotocol");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.printingProtocol = value;
  }
}

package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the provider
 * <p/>
 * Tag: Provider
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class ProviderHandler extends AbstractADFHeaderHandler {
  public ProviderHandler() {
    setTag("provider");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.provider = value;
  }
}

package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the substrate type
 * <p/>
 * Tag: Substrate Type
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class SubstrateTypeHandler extends AbstractADFHeaderHandler {
  public SubstrateTypeHandler() {
    setTag("substratetype");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.substrateType.add(value);
  }
}

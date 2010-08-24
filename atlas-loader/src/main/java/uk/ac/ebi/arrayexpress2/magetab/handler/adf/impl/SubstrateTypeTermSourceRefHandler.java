package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the substrate type term source
 * reference
 * <p/>
 * Tag: Substrate Type Term Source REF
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class SubstrateTypeTermSourceRefHandler
    extends AbstractADFHeaderHandler {
  public SubstrateTypeTermSourceRefHandler() {
    setTag("substratetypetermsourceref");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.substrateTypeTermSourceRef.add(value);
  }
}

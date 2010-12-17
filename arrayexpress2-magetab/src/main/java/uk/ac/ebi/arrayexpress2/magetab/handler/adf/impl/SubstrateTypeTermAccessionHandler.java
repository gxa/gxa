package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the substrate type term accession
 * <p/>
 * Tag: Substrate Type Term Accession Number
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class SubstrateTypeTermAccessionHandler
    extends AbstractADFHeaderHandler {
  public SubstrateTypeTermAccessionHandler() {
    setTag("substratetypetermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.substrateTypeTermAccession.add(value);
  }
}

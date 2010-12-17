package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles the term source version declaration rows in ADF. These
 * elements are tied to a term source name.
 * <p/>
 * Tag: Term Source Version
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class TermSourceVersionHandler extends AbstractADFHeaderHandler {
  public TermSourceVersionHandler() {
    setTag("termsourceversion");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.termSourceVersion.add(value);
  }
}

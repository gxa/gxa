package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles the term source name declaration rows in ADF.  This
 * row declared names that are used within this document to refer to a
 * particular term source.
 * <p/>
 * Tag: Term Source Name
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class TermSourceNameHandler extends AbstractADFHeaderHandler {
  public TermSourceNameHandler() {
    setTag("termsourcename");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.termSourceName.add(value);
  }
}

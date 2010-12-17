package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handlers ADF rows describing the array design name.
 * <p/>
 * Tag: Array Design Name
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class ArrayDesignNameHandler extends AbstractADFHeaderHandler {
  public ArrayDesignNameHandler() {
    setTag("arraydesignname");
    setAllowedLength(1);
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.arrayDesignName = value;
  }
}

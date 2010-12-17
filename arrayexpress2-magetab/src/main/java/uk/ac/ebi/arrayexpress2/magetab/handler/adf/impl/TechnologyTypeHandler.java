package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the technology type
 * <p/>
 * Tag: Technology Type
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class TechnologyTypeHandler extends AbstractADFHeaderHandler {
  public TechnologyTypeHandler() {
    setTag("technologytype");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.technologyType.add(value);
  }
}

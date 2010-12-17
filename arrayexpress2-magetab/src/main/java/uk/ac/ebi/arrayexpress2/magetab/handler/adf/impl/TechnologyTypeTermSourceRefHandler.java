package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the technology type term source
 * reference
 * <p/>
 * Tag: Technology Type Term Source REF
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class TechnologyTypeTermSourceRefHandler
    extends AbstractADFHeaderHandler {
  public TechnologyTypeTermSourceRefHandler() {
    setTag("technologytypetermsourceref");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.technologyTypeTermSourceRef.add(value);
  }
}

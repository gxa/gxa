package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the surface type term source
 * reference
 * <p/>
 * Tag: Surface Type Term Source REF
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class SurfaceTypeTermSourceRefHandler extends AbstractADFHeaderHandler {
  public SurfaceTypeTermSourceRefHandler() {
    setTag("surfacetypetermsourceref");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.surfaceTypeTermSourceRef.add(value);
  }
}

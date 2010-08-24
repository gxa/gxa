package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the surface type
 * <p/>
 * Tag: Surface Type
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class SurfaceTypeHandler extends AbstractADFHeaderHandler {
  public SurfaceTypeHandler() {
    setTag("surfacetype");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.surfaceType.add(value);
  }
}

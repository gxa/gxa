package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the surface type term accession
 * <p/>
 * Tag: Surface Type Term Accession Number
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class SurfaceTypeTermAccessionHandler extends AbstractADFHeaderHandler {
  public SurfaceTypeTermAccessionHandler() {
    setTag("surfacetypetermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.surfaceTypeTermAccession.add(value);
  }
}

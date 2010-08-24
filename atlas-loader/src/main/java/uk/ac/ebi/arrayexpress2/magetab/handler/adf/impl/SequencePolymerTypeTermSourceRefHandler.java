package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the sequence polymer type term
 * source reference
 * <p/>
 * Tag: Sequence Polymer Type Term Source REF
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class SequencePolymerTypeTermSourceRefHandler
    extends AbstractADFHeaderHandler {
  public SequencePolymerTypeTermSourceRefHandler() {
    setTag("sequencepolymertypetermsourceref");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.sequencePolymerTypeTermSourceRef.add(value);
  }
}

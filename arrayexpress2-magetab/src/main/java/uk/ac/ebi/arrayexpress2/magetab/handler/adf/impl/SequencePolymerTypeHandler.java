package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the sequence polymer type
 * <p/>
 * Tag: Sequence Polymer Type
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class SequencePolymerTypeHandler extends AbstractADFHeaderHandler {
  public SequencePolymerTypeHandler() {
    setTag("sequencepolymertype");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.sequencePolymerType.add(value);
  }
}

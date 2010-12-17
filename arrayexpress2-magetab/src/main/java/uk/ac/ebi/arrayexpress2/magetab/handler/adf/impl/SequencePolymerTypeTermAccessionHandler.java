package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the sequence polymer type term
 * accession
 * <p/>
 * Tag: Sequence Polymer Type Term Accession Number
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class SequencePolymerTypeTermAccessionHandler
    extends AbstractADFHeaderHandler {
  public SequencePolymerTypeTermAccessionHandler() {
    setTag("sequencepolymertermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.sequencePolymerTypeTermAccession.add(value);
  }
}

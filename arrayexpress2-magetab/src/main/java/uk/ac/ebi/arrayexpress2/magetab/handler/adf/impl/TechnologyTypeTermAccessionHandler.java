package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A handler that handles ADF rows describing the technology type term
 * accession
 * <p/>
 * Tag: Technology Type Term Accession Number
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class TechnologyTypeTermAccessionHandler
    extends AbstractADFHeaderHandler {
  public TechnologyTypeTermAccessionHandler() {
    setTag("technologytypetermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.technologyTypeTermAccession.add(value);
  }
}

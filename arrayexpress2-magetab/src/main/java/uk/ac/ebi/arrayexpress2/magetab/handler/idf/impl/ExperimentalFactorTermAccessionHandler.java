package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Experimental Factor Term Accession
 * fields in the IDF.
 * <p/>
 * Tag: Experimental Factor Term Accession Number
 *
 * @author Tony Burdett
 * @date 17-May-2010
 */
public class ExperimentalFactorTermAccessionHandler extends AbstractIDFHandler {
  public ExperimentalFactorTermAccessionHandler() {
    setTag("experimentalfactortermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.experimentalFactorTermAccession.add(value);
  }
}

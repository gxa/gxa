package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Experimental Factor Term Source REF
 * fields in the IDF.
 * <p/>
 * Tag: Experimental Factor Term Source Ref
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ExperimentalFactorTermSourceRefHandler extends AbstractIDFHandler {
  public ExperimentalFactorTermSourceRefHandler() {
    setTag("experimentalfactortermsourceref");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.experimentalFactorTermSourceREF.add(value);
  }
}

package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Experimental Factor Type fields in
 * the IDF.
 * <p/>
 * Tag: Experimental Factor Type
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ExperimentalFactorTypeHandler extends AbstractIDFHandler {
  public ExperimentalFactorTypeHandler() {
    setTag("experimentalfactortype");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.experimentalFactorType.add(value);
  }
}

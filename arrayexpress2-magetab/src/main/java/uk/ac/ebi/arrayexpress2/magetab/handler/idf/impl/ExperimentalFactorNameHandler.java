package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Experimental Factor Name fields in
 * the IDF.
 * <p/>
 * Tag: Experimental Factor Name
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ExperimentalFactorNameHandler extends AbstractIDFHandler {
  public ExperimentalFactorNameHandler() {
    setTag("experimentalfactorname");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.experimentalFactorName.add(value);
  }
}

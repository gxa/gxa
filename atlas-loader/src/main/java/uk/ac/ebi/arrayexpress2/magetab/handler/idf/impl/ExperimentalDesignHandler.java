package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Experimental Design fields in the
 * IDF.
 * <p/>
 * Tag: Experimental Design
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ExperimentalDesignHandler extends AbstractIDFHandler {
  public ExperimentalDesignHandler() {
    setTag("experimentaldesign");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.experimentalDesign.add(value);
  }
}

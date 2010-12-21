package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Experimental Design Term Source Ref
 * fields in the IDF.
 * <p/>
 * Tag: Experimental Design Term Source Ref
 *
 * @author Tony Burdett
 * @date 18-Mar-2009
 */
public class ExperimentalDesignTermSourceRefHandler extends AbstractIDFHandler {
  public ExperimentalDesignTermSourceRefHandler() {
    setTag("experimentaldesigntermsourceref");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.experimentalDesignTermSourceREF.add(value);
  }
}

package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Experiment Description fields in
 * the IDF.
 * <p/>
 * Tag: Experiment Description
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ExperimentDescriptionHandler extends AbstractIDFHandler {
  public ExperimentDescriptionHandler() {
    setTag("experimentdescription");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.experimentDescription = value;
  }
}

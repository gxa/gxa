package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Date of Experiment fields in the
 * IDF.
 * <p/>
 * Tag: Date of Experiment
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class DateOfExperimentHandler extends AbstractIDFHandler {
  public DateOfExperimentHandler() {
    setTag("dateofexperiment");
    setAllowedLength(1);
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.dateOfExperiment = value;
  }
}

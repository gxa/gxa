package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Quality Control Type fields in the
 * IDF.
 * <p/>
 * Tag: Quality Control Type
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class QualityControlTypeHandler extends AbstractIDFHandler {
  public QualityControlTypeHandler() {
    setTag("qualitycontroltype");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.qualityControlType.add(value);
  }
}

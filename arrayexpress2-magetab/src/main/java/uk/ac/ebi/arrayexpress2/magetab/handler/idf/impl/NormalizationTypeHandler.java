package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Normalization Type fields in the
 * IDF.
 * <p/>
 * Tag: Normalization Type
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class NormalizationTypeHandler extends AbstractIDFHandler {
  public NormalizationTypeHandler() {
    setTag("normalizationtype");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.normalizationType.add(value);
  }
}

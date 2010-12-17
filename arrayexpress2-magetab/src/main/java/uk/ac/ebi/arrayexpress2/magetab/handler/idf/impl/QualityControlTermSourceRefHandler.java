package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Quality Control Term Source Ref
 * fields in the IDF.
 * <p/>
 * Tag: Quality Control Term Source Ref
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class QualityControlTermSourceRefHandler extends AbstractIDFHandler {
  public QualityControlTermSourceRefHandler() {
    setTag("qualitycontroltermsourceref");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.qualityControlTermSourceREF.add(value);
  }
}

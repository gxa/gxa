package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Quality Control Term Accession
 * fields in the IDF.
 * <p/>
 * Tag: Quality Control Term Accession
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class QualityControlTermAccessionHandler extends AbstractIDFHandler {
  public QualityControlTermAccessionHandler() {
    setTag("qualitycontroltermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.qualityControlTermAccession.add(value);
  }
}

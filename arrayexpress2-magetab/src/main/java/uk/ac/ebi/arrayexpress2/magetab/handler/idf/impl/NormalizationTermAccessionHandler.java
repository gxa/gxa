package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Normalization Term Accession fields
 * in the IDF.
 * <p/>
 * Tag: Normalization Term Accession Number
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class NormalizationTermAccessionHandler extends AbstractIDFHandler {
  public NormalizationTermAccessionHandler() {
    setTag("normalizationtermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.normalizationTermAccession.add(value);
  }
}

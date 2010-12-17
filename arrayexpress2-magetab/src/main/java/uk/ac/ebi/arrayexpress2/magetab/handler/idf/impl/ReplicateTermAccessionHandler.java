package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Replicate Term Accession fields in
 * the IDF.
 * <p/>
 * Tag: Replicate Term Accession Number
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class ReplicateTermAccessionHandler extends AbstractIDFHandler {
  public ReplicateTermAccessionHandler() {
    setTag("replicatetermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.replicateTermAccession.add(value);
  }
}

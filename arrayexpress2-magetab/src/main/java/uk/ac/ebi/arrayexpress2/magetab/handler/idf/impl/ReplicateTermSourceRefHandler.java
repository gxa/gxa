package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Replicate Term Source Ref fields in
 * the IDF.
 * <p/>
 * Tag: Replicate Term Source Ref
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ReplicateTermSourceRefHandler extends AbstractIDFHandler {
  public ReplicateTermSourceRefHandler() {
    setTag("replicatetermsourceref");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.replicateTermSourceREF.add(value);
  }
}

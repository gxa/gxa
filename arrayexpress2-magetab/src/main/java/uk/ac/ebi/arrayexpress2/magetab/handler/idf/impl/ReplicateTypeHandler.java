package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Replicate Types in the IDF.
 * <p/>
 * Tag: Replicate Type
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class ReplicateTypeHandler extends AbstractIDFHandler {
  public ReplicateTypeHandler() {
    setTag("replicatetype");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.replicateType.add(value);
  }
}

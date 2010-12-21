package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Experimental Design Term Accession
 * Number fields in the IDF.
 * <p/>
 * Tag: Experimental Design Term Accession Number
 *
 * @author Tony Burdett
 * @date 03-Jun-2010
 */
public class ExperimentalDesignTermAccessionHandler extends AbstractIDFHandler {
  public ExperimentalDesignTermAccessionHandler() {
    setTag("experimentaldesigntermaccessionnumber");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.experimentalDesignTermAccession.add(value);
  }
}

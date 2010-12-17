package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler that handles the line in the IDF that references the SDRF file
 * location.  This should be a path, relative to the IDF file, that resolves to
 * the SDRF.
 * <p/>
 * Tag: SDRF File
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class SDRFFileHandler extends AbstractIDFHandler {
  public SDRFFileHandler() {
    setTag("sdrffile");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.sdrfFile.add(value);
  }
}

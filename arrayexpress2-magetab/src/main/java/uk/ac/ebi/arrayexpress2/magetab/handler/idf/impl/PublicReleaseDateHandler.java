package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A handler implementation that will handle Public Release Date Handler fields
 * in the IDF.
 * <p/>
 * Tag: Public Release Date
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class PublicReleaseDateHandler extends AbstractIDFHandler {
  public PublicReleaseDateHandler() {
    setTag("publicreleasedate");
    setAllowedLength(1);
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.publicReleaseDate = value;
  }
}

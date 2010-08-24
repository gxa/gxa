package uk.ac.ebi.arrayexpress2.magetab.handler;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;

/**
 * A type of {@link Handler} that enables read functionality over a lump of
 * data.
 *
 * @author Tony Burdett
 * @date 01-May-2009
 */
public interface ReadHandler extends Handler {
  /**
   * Cause the handler to read from it's source into some internal buffer.
   *
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if parsing failed
   */
  void read() throws ParseException;
}

package uk.ac.ebi.arrayexpress2.magetab.handler;

import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;

/**
 * A type of {@link Handler} that enables read functionality over a lump of
 * data.
 *
 * @author Tony Burdett
 * @date 01-May-2009
 */
public interface ValidateHandler extends Handler {
  /**
   * Causes the handler to validate it's data.  This may need other data to be
   * available in a cache to validate against.
   *
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException
   *          if there is a problem validating objects
   */
  void validate() throws ObjectConversionException;
}

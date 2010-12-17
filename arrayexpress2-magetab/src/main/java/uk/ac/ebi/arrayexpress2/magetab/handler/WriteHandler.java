package uk.ac.ebi.arrayexpress2.magetab.handler;

import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;

/**
 * A type of {@link Handler} that enables write functionality over a lump of
 * data.
 *
 * @author Tony Burdett
 * @date 01-May-2009
 */
public interface WriteHandler extends Handler {
  /**
   * Causes the handler to write objects out based on data in the internal
   * buffer.  This data can usually be accessed directly from a cache or by
   * calling a method on the specific implementing class.
   *
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException
   *          if data in the buffer could not be converted
   */
  void write() throws ObjectConversionException;
}

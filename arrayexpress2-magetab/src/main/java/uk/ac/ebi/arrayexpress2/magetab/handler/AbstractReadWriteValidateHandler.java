package uk.ac.ebi.arrayexpress2.magetab.handler;

import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;

/**
 * An abstract reading, writing and validating handler implementation. This
 * implementation performs read and write functionality, followed by
 * validation.
 *
 * @author Tony Burdett
 * @date 01-May-2009
 */
public abstract class AbstractReadWriteValidateHandler
    extends AbstractHandler
    implements ReadHandler, WriteHandler, ValidateHandler {
  public AbstractReadWriteValidateHandler() {
    mode = ParserMode.READ_AND_WRITE;
  }

  /**
   * Invoke the current read and write handler.  Because this is an
   * implementation of a ReadHandler and a WriteHandler, this will have no
   * validation effects if requested.  It will read in READ_ONLY mode, write in
   * WRITE_ONLY mode but never validate.
   *
   * @throws ParseException            if the handler failed to read data from
   *                                   it's source
   * @throws ObjectConversionException if the handler failed to write out
   *                                   appropriate objects
   */
  public void handle() throws ParseException, ObjectConversionException {
    switch (mode) {
      default:
      case READ_AND_WRITE:
        read();
        write();
        validate();
        break;
      case READ_ONLY:
        read();
        validate();
        break;
      case WRITE_ONLY:
        write();
        validate();
        break;
    }
  }
}

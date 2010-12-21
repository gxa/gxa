package uk.ac.ebi.arrayexpress2.magetab.handler;

import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;

/**
 * An abstract reading handler implementation. This implementation only performs
 * read functionality, irrespective of the requested mode.
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public abstract class AbstractReadHandler
    extends AbstractHandler
    implements ReadHandler {
  public AbstractReadHandler() {
    mode = ParserMode.READ_ONLY;
  }


  /**
   * Invoke the current read handler.  Because this is only an implementation of
   * a ReadHandler, this will have no write effects if requested.  hence, if
   * running this handler in Mode.READ_AND_WRITE, this will invoke {@link
   * #read()} only. In Mode.READ_ONLY mode, this will simply invoke {@link
   * #read()} and in Mode.WRITE_ONLY mode nothing will happen.
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
        getLog()
            .warn("This Handler (" + getClass().getSimpleName() + ") is a " +
                "READ_ONLY handler - running in READ_AND_WRITE mode won't " +
                "have any WRITE effects");
        read();
        break;
      case READ_ONLY:
        read();
        break;
      case WRITE_ONLY:
        getLog()
            .error("This Handler (" + getClass().getSimpleName() + ") is a " +
                "READ_ONLY handler, so WRITE_ONLY mode will do nothing");
        break;
    }
  }
}

package uk.ac.ebi.arrayexpress2.magetab.handler;

import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;

/**
 * An abstract writing handler implementation. This implementation only performs
 * write functionality, irrespective of the requested mode.
 *
 * @author Tony Burdett
 * @date 01-May-2009
 */
public abstract class AbstractWriteHandler
    extends AbstractHandler
    implements WriteHandler {
  public AbstractWriteHandler() {
    mode = ParserMode.WRITE_ONLY;
  }

  /**
   * Invoke the current read handler.  Because this is only an implementation of
   * a WriteHandler, this will have no read effects if requested.  hence, if
   * running this handler in Mode.READ_AND_WRITE, this will invoke {@link
   * #write()} only. In Mode.WRITE_ONLY mode, this will simply invoke {@link
   * #write()} and in Mode.WRITE_ONLY mode nothing will happen.
   *
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if the handler failed to read data from it's source
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException
   *          if the handler failed to write out appropriate objects
   */
  public void handle() throws ParseException, ObjectConversionException {
    switch (mode) {
      default:
      case READ_AND_WRITE:
        getLog()
            .warn("This Handler (" + getClass().getSimpleName() + ") is a " +
                "WRITE_ONLY handler - running in READ_AND_WRITE mode won't " +
                "have any READ effects");
        write();
        break;
      case READ_ONLY:
        getLog()
            .error("This Handler (" + getClass().getSimpleName() + ") is a " +
                "READ_ONLY handler, so WRITE_ONLY mode will do nothing");
        break;
      case WRITE_ONLY:
        write();
        break;
    }
  }
}

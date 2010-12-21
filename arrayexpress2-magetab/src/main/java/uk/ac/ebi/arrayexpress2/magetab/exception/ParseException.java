package uk.ac.ebi.arrayexpress2.magetab.exception;

import org.mged.magetab.error.ErrorItem;

/**
 * An exception thrown whenever there was a problem parsing a native MAGE-TAB
 * file.  This would only ever occur during the read phase.
 * <p/>
 * ParseExceptions require an ErrorItem to be passed into the constructor.
 * Whenever you wish to throw a ParseException, you should create an ErrorItem
 * that encapsulates the error message along with a code for the error and
 * information about the location in the file that caused the problem to occur.
 * Then, any error listeners can utilise this object to provide user feedback.
 *
 * @author Tony Burdett
 * @date 10-Feb-2009
 */
public class ParseException extends Exception {
  private ErrorItem error;
  private boolean isCritical;

  public ParseException(ErrorItem error, boolean isCritical) {
    super();
    this.error = error;
    this.isCritical = isCritical;
  }

  public ParseException(ErrorItem error, boolean isCritical, String s) {
    super(s);
    this.error = error;
    this.isCritical = isCritical;
  }

  public ParseException(ErrorItem error, boolean isCritical, String s,
                        Throwable throwable) {
    super(s, throwable);
    this.error = error;
    this.isCritical = isCritical;
  }

  public ParseException(ErrorItem error, boolean isCritical,
                        Throwable throwable) {
    super(throwable);
    this.error = error;
    this.isCritical = isCritical;
  }

  public ErrorItem getErrorItem() {
    return error;
  }

  public boolean isCriticalException() {
    return isCritical;
  }
}

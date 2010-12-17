package uk.ac.ebi.arrayexpress2.magetab.exception;

import org.mged.magetab.error.ErrorItem;

/**
 * An exception that gets thrown whenever an object in the internal MAGE-TAB
 * datamodel cannot be converted into an equivalent object in the AE2 database
 * schema driven model.  This would only ever occur during the write phase.
 * <p/>
 * ObjectConversionExceptions require an ErrorItem to be passed into the
 * constructor.  Whenever you wish to throw a ObjectConversionException, you
 * should create an ErrorItem that encapsulates the error message along with a
 * code for the error and information about the location in the file that caused
 * the problem to occur.  Then, any error listeners can utilise this object to
 * provide user feedback.
 *
 * @author Tony Burdett
 * @date 10-Feb-2009
 */
public class ObjectConversionException extends Exception {
  private ErrorItem error;
  private boolean isCritical;

  public ObjectConversionException(ErrorItem error, boolean isCritical) {
    super();
    this.error = error;
    this.isCritical = isCritical;
  }

  public ObjectConversionException(ErrorItem error, boolean isCritical,
                                   String s) {
    super(s);
    this.error = error;
    this.isCritical = isCritical;
  }

  public ObjectConversionException(ErrorItem error, boolean isCritical,
                                   String s, Throwable throwable) {
    super(s, throwable);
    this.error = error;
    this.isCritical = isCritical;
  }

  public ObjectConversionException(ErrorItem error, boolean isCritical,
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

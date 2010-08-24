package uk.ac.ebi.arrayexpress2.magetab.exception;

import org.mged.magetab.error.ErrorItem;

/**
 * A {@link ParseException} that is thrown whenever a line parsed from a TDT
 * file is an illegal length.  An illegal length is considered to be one that is
 * not expected, either because it is not allowed in the spec, or because it is
 * different from the length of other dependent lines.
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class IllegalLineLengthException extends ParseException {
  public IllegalLineLengthException(ErrorItem error, boolean isCritical) {
    super(error, isCritical);
  }

  public IllegalLineLengthException(ErrorItem error, boolean isCritical,
                                    String s) {
    super(error, isCritical, s);
  }

  public IllegalLineLengthException(ErrorItem error, boolean isCritical,
                                    String s,
                                    Throwable throwable) {
    super(error, isCritical, s, throwable);
  }

  public IllegalLineLengthException(ErrorItem error, boolean isCritical,
                                    Throwable throwable) {
    super(error, isCritical, throwable);
  }
}

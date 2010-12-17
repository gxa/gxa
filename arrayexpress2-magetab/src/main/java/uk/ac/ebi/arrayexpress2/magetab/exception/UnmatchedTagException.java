package uk.ac.ebi.arrayexpress2.magetab.exception;

import org.mged.magetab.error.ErrorItem;

/**
 * A {@link ParseException} that gets thrown whenever a tag in a TDT file is
 * mismatched when compared to the expected one, or if the content type is wrong
 * for the header.
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class UnmatchedTagException extends ParseException {
  public UnmatchedTagException(ErrorItem error, boolean isCritical) {
    super(error, isCritical);
  }

  public UnmatchedTagException(ErrorItem error, boolean isCritical, String s) {
    super(error, isCritical, s);
  }

  public UnmatchedTagException(ErrorItem error, boolean isCritical, String s,
                               Throwable throwable) {
    super(error, isCritical, s, throwable);
  }

  public UnmatchedTagException(ErrorItem error, boolean isCritical,
                               Throwable throwable) {
    super(error, isCritical, throwable);
  }
}

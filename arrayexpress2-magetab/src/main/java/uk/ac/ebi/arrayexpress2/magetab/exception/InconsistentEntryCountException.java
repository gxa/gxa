package uk.ac.ebi.arrayexpress2.magetab.exception;

import org.mged.magetab.error.ErrorItem;

/**
 * A {@link ParseException} that is thrown when the number of entries in a
 * spreadsheet is not consistent, for example when an fixed size matrix is
 * expected but column lengths are variable.
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class InconsistentEntryCountException extends ParseException {
  public InconsistentEntryCountException(ErrorItem error, boolean isCritical) {
    super(error, isCritical);
  }

  public InconsistentEntryCountException(ErrorItem error, boolean isCritical,
                                         String s) {
    super(error, isCritical, s);
  }

  public InconsistentEntryCountException(ErrorItem error, boolean isCritical,
                                         String s,
                                         Throwable throwable) {
    super(error, isCritical, s, throwable);
  }

  public InconsistentEntryCountException(ErrorItem error, boolean isCritical,
                                         Throwable throwable) {
    super(error, isCritical, throwable);
  }
}

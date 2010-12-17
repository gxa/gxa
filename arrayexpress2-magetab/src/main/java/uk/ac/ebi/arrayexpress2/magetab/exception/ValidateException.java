package uk.ac.ebi.arrayexpress2.magetab.exception;

import org.mged.magetab.error.ErrorItem;

/**
 * A {@link ParseException} that gets thrown whenever part of a {@link
 * uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation} or a {@link
 * uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation} fails a
 * validation process.  Usually this will be thrown by implementations of the
 * {@link uk.ac.ebi.arrayexpress2.magetab.validator.Validator} interface.
 *
 * @author Tony Burdett
 * @date 02-Jun-2009
 */
public class ValidateException extends ParseException {
  public ValidateException(ErrorItem error, boolean isCritical) {
    super(error, isCritical);
  }

  public ValidateException(ErrorItem error, boolean isCritical, String s) {
    super(error, isCritical, s);
  }

  public ValidateException(ErrorItem error, boolean isCritical, String s,
                           Throwable throwable) {
    super(error, isCritical, s, throwable);
  }

  public ValidateException(ErrorItem error, boolean isCritical,
                           Throwable throwable) {
    super(error, isCritical, throwable);
  }
}

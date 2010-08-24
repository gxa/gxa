package uk.ac.ebi.arrayexpress2.magetab.validator;

import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.exception.ValidateException;

import java.util.Set;

/**
 * A semantic validator process, distinct from syntactic validation which is
 * handled by a {@link uk.ac.ebi.arrayexpress2.magetab.parser.Parser}.
 * Implementations of this class should be typed by the types of object they
 * validate.
 *
 * @author Tony Burdett
 * @date 02-Jun-2009
 */
public interface Validator<T> {
  /**
   * Return the progress of the current import process, as a percentage.  This
   * should be the actual progress of the current job, or -1 if it is
   * indeterminate.
   *
   * @return the current progress of the current import process
   */
  int getProgress();

  /**
   * Register an {@link ErrorItemListener} with this parser.  Whenever an error
   * is encountered when parsing from the a parserSource, this listener should
   * be notified.
   *
   * @param listener the listener to register
   */
  void addErrorItemListener(ErrorItemListener listener);

  /**
   * Deregister an {@link ErrorItemListener} with this parser.  This listener
   * will no longer be notified when error items are generated.
   *
   * @param listener the listener to register
   */
  void removeErrorItemListener(ErrorItemListener listener);

  /**
   * Get the current set of {@link uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener}s
   * registered to this validator
   *
   * @return the current set of listeners registered
   */
  Set<ErrorItemListener> getErrorItemListeners();

  /**
   * Perform validation of the supplied datamodel of generic type.  This will
   * return true if the model is valid, false is it is invalid, and throw a
   * ValidationException if something went wrong during the validation.
   *
   * @param validatorSource the generically typed datamodel object to validate
   * @return the data model that has been populated by this import operation
   * @throws ValidateException if validation could not complete.  This
   *                           represents a failure during validation, not
   *                           simply an invalid source
   */
  boolean validate(T validatorSource) throws ValidateException;
}

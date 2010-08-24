package uk.ac.ebi.arrayexpress2.magetab.parser;

import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.validator.Validator;

import java.net.URL;
import java.util.Set;

/**
 * A parser that reads from a given URL and populates a data model object.  The
 * data model is of a generic type specified by the parameter on implementing
 * classes.
 *
 * @author Tony Burdett
 * @date 02-Apr-2009
 */
public interface Parser<T> {
  /**
   * Return the progress of the current import process, as a percentage.  This
   * should be the actual progress of the current job, or -1 if it is
   * indeterminate.
   *
   * @return the current progress of the current import process
   */
  int getProgress();

  /**
   * Configure this parser with a {@link Validator}.  Normally, the validate()
   * method on the Validator will be called once parsing has completed.  It is
   * therefore possible to share an ErrorItemListener between the Parser and
   * Validator
   *
   * @param validator the validator this parser should use to validate once
   *                  parsing has completed.
   */
  void setValidator(Validator<T> validator);

  /**
   * Get the currently configured validator for this parser, or null if there
   * has not been one set.
   *
   * @return the Validator for this parser
   */
  Validator<T> getValidator();

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
   * registered to this parser
   *
   * @return the current set of listeners registered
   */
  Set<ErrorItemListener> getErrorItemListeners();

  /**
   * Perform parsing from the supplied URL to a new data model instance.
   *
   * @param parserSource the URL to parse from
   * @return the data model that has been populated by this import operation
   * @throws ParseException if the source format was invalid, or the URL could
   *                        not be read.
   */
  T parse(URL parserSource) throws ParseException;
}

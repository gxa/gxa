package uk.ac.ebi.arrayexpress2.magetab.parser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mged.magetab.error.ErrorItem;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.validator.Validator;

import java.util.HashSet;
import java.util.Set;

/**
 * An abstract basic implementation of a {@link Parser}.  This simply handles
 * progress updating, validator setting and listener registration.
 * <p/>
 * Note that this implementation will cascase any registered listeners to the
 * validator as well.  This means that listeners registered to the parser will
 * also report on validation errors, removing the requirement to separately
 * handle parse and validation errors.
 *
 * @author Tony Burdett
 * @date 02-Apr-2009
 */
public abstract class AbstractParser<T> implements Parser<T> {
  private int progress = -1;

  private Set<ErrorItemListener> listeners;
  private Validator<T> validator;

  // logging
  private Log log = LogFactory.getLog(this.getClass());

  public Log getLog() {
    return log;
  }

  /**
   * Update the progress of the current import.  If not specified, this defaults
   * to -1, which represents and indeterminate task.  However, once import is
   * complete you should set the progress to 100.
   *
   * @param progress the progress to set
   */
  public synchronized void updateProgress(int progress) {
    this.progress = progress;
    notifyAll();
  }

  /**
   * Returns the current progress of the import.
   *
   * @return the percentage progress.
   */
  public synchronized int getProgress() {
    return progress;
  }

  public void setValidator(Validator<T> validator) {
    // remove listeners on the parser from the old validator first,
    // so it can be gc'ed if necessary
    if (validator != null) {
      for (ErrorItemListener listener : getErrorItemListeners()) {
        validator.removeErrorItemListener(listener);
      }
    }

    // set the new validator
    this.validator = validator;

    // register listeners on the parser to the validator as well
    for (ErrorItemListener listener : getErrorItemListeners()) {
      this.validator.addErrorItemListener(listener);
    }
  }

  public Validator<T> getValidator() {
    return validator;
  }

  public void addErrorItemListener(ErrorItemListener listener) {
    if (listeners == null) {
      listeners = new HashSet<ErrorItemListener>();
    }
    listeners.add(listener);

    // also register to the validator, if present
    if (validator != null) {
      validator.addErrorItemListener(listener);
    }
  }

  public void removeErrorItemListener(ErrorItemListener listener) {
    if (listeners != null && listeners.contains(listener)) {
      listeners.remove(listener);
    }

    // also remove from the validator, if present
    if (validator != null) {
      validator.removeErrorItemListener(listener);
    }
  }

  public Set<ErrorItemListener> getErrorItemListeners() {
    if (listeners == null) {
      listeners = new HashSet<ErrorItemListener>();
    }
    return listeners;
  }

  /**
   * Fires an error item notification on any {@link ErrorItemListener}s
   * currently configured to listen to this parser.
   *
   * @param errorItem the error item that encapsulates the error
   */
  protected void fireErrorItemEvent(ErrorItem errorItem) {
    for (ErrorItemListener listener : getErrorItemListeners()) {
      listener.errorOccurred(errorItem);
    }
  }
}

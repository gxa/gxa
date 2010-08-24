package uk.ac.ebi.arrayexpress2.magetab.validator;

import org.mged.magetab.error.ErrorItem;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;

import java.util.HashSet;
import java.util.Set;

/**
 * An abstract implementation of the {@link uk.ac.ebi.arrayexpress2.magetab.validator.Validator}
 * interface.  This class supplies default implementations of progress tracking
 * and listener registration, so that concrete implementations need only call
 * {@link #fireErrorItemEvent(org.mged.magetab.error.ErrorItem)} to report an
 * event whenever an error item is generated.
 *
 * @author Tony Burdett
 * @date 02-Jun-2009
 */
public abstract class AbstractValidator<T> implements Validator<T> {
  private int progress = -1;

  private Set<ErrorItemListener> listeners;

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

  public void addErrorItemListener(ErrorItemListener listener) {
    if (listeners == null) {
      listeners = new HashSet<ErrorItemListener>();
    }
    listeners.add(listener);
  }

  public void removeErrorItemListener(ErrorItemListener listener) {
    if (listeners != null && listeners.contains(listener)) {
      listeners.remove(listener);
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
   * currently configured to listen to this validator.
   *
   * @param errorItem the error item that encapsulates the error
   */
  protected void fireErrorItemEvent(ErrorItem errorItem) {
    for (ErrorItemListener listener : getErrorItemListeners()) {
      listener.errorOccurred(errorItem);
    }
  }
}

package uk.ac.ebi.arrayexpress2.magetab.exception;

import org.mged.magetab.error.ErrorItem;

import java.util.EventListener;

/**
 * An {@link EventListener} interface for making error item callbacks whenever
 * an error item occurs.
 *
 * @author Tony Burdett
 * @date 02-Jun-2009
 */
public interface ErrorItemListener extends EventListener {
  /**
   * Invoked whenever an error item is generated during a parsing operation.
   *
   * @param item the item describing the error that was generated
   */
  void errorOccurred(ErrorItem item);
}

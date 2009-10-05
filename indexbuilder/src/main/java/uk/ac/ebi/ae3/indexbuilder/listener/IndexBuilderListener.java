package uk.ac.ebi.ae3.indexbuilder.listener;

import java.util.EventListener;

/**
 * A Listener that can be used to determine when an IndexBuilder has completed
 * it's execution.
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public interface IndexBuilderListener extends EventListener {
  /**
   * Indicates that building or updating of an index completed successfully
   *
   * @param event the event representing this build success event
   */
  public void buildSuccess(IndexBuilderEvent event);

  /**
   * Indicates that building or updating of an index exited with an error
   *
   * @param event the event representing this build failure
   */
  public void buildError(IndexBuilderEvent event);
}

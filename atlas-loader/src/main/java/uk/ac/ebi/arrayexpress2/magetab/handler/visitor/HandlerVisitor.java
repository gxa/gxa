package uk.ac.ebi.arrayexpress2.magetab.handler.visitor;

import uk.ac.ebi.arrayexpress2.magetab.handler.Handler;

/**
 * An visitor interface that can inspect {@link Handler}s, and extract useful
 * information about the data that was assigned to it.  This interface is typed
 * by the type of handlers it can visit.
 *
 * @author Tony Burdett
 * @date 23-Nov-2009
 */
public interface HandlerVisitor<T extends Handler> {
  /**
   * Visit a Handler and extract relevant information
   *
   * @param handler the Handler to visit
   */
  void visit(T handler);
}

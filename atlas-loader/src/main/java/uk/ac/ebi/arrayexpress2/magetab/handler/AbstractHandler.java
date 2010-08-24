package uk.ac.ebi.arrayexpress2.magetab.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.handler.visitor.HandlerVisitor;

/**
 * A basic handler implementation that provides some simple functionality common
 * to all handlers.  There is no implementation of the <code>handle()</code>
 * method so more specific implementations can override this.
 *
 * @author Tony Burdett
 * @date 01-May-2009
 */
public abstract class AbstractHandler implements Handler {
  protected ParserMode mode = ParserMode.READ_ONLY;

  // for calculating progress
  protected double increase;
  protected int taskIndex;

  // values that should be overriden by implementations
  protected String tag;
  protected int allowedLength = Integer.MAX_VALUE;

  // logging
  private Log log = LogFactory.getLog(this.getClass());

  public Log getLog() {
    return log;
  }

  /**
   * Get the tag (the column header in the SDRF, or the first token on a line in
   * IDF) for the data this handler can deal with
   *
   * @return the tag that this handler can handle
   */
  public String getTag() {
    return tag;
  }

  /**
   * Set the tag for the data this handler can deal with
   *
   * @param tag the tag this handler can handle
   */
  protected void setTag(String tag) {
    this.tag = tag;
  }

  /**
   * Get the legal number of data entries for this type of handler, as specified
   * by the MAGE-TAB specification.  Usually, this will not be set unless there
   * is a specific cardinality=1 requirement (only found in some places in the
   * IDF spec).
   *
   * @return the allowed length
   */
  public int getAllowedLength() {
    return allowedLength;
  }

  /**
   * Set the legal number of data entries for this type of handler, as specified
   * by the MAGE-TAB specification.  Usually, this will not be set unless there
   * is a specific cardinality=1 requirement (only found in some places in the
   * IDF spec).
   *
   * @param allowedLength the allowed length
   */
  protected void setAllowedLength(int allowedLength) {
    this.allowedLength = allowedLength;
  }

  public void setHandlerMode(ParserMode mode) {
    this.mode = mode;
  }

  public ParserMode getHandlerMode() {
    return mode;
  }

  public int getTaskIndex() {
    return taskIndex;
  }

  public void setTaskIndex(int taskIndex) {
    this.taskIndex = taskIndex;
  }

  public void increasesProgressBy(double increase) {
    this.increase = increase;
  }

  public String handlesTag() {
    return tag;
  }

  public boolean canHandle(String tag) {
    return this.tag.equals(tag);
  }

  public void accept(HandlerVisitor visitor) {
    visitor.visit(this);
  }
}

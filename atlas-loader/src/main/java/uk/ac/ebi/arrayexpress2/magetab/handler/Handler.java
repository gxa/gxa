package uk.ac.ebi.arrayexpress2.magetab.handler;

import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.visitor.HandlerVisitor;

/**
 * A class for handling discrete lumps of a MAGE-TAB data that can be mostly
 * dealt with in isolation. Generally handlers are functional - i.e. handling a
 * single line from a text file with reference to a tag or set of tags.
 * <p/>
 * Handlers should generally take some data and {@link #handle()} it.  This
 * interface can be extended to provide handlers with read, write and validate
 * functionality.
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 * @see uk.ac.ebi.arrayexpress2.magetab.handler.idf.IDFHandler
 * @see uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.SDRFHandler
 * @see uk.ac.ebi.arrayexpress2.magetab.handler.adf.ADFHandler
 */
public interface Handler {
  /**
   * Set the mode of operation for this handler.  READ_AND_WRITE by default.
   *
   * @param mode the handler mode
   */
  void setHandlerMode(ParserMode mode);

  /**
   * Get the mode of operation for this handler.  READ_AND_WRITE unless
   * otherwise specified.
   *
   * @return the handler mode
   */
  ParserMode getHandlerMode();

  /**
   * Set the index number of this task.  This is used to track tasks being
   * performed in parallel, and to allow monitoring of the status of tasks.
   *
   * @param taskIndex the index this handler task is assigned
   */
  void setTaskIndex(int taskIndex);

  /**
   * Get the index number of this task.  This is used in tracking of handler
   * tasks when several are being run in parallel.
   *
   * @return the index of this task
   */
  int getTaskIndex();

  /**
   * Set an amount that this handler should increase the progress of the total
   * MAGETABInvestigation by upon completion, out of 100.  This should simply
   * reflect the number of handlers - for example, if there are 10 handlers,
   * each handler should increase the progress by 10 points upon completion.
   * Optionally, handlers can do incremental updates (e.g. updating the
   * investigation by 1 point if there are 10 iterations).
   *
   * @param increase the percentage of the total progress this handler is
   *                 responsible for
   */
  void increasesProgressBy(double increase);

  /**
   * The tag describing the entries that this handler will handle.  This will
   * return the common string representation of the tag in an IDF, SDRF or ADF
   * file that this handler can take.
   *
   * @return the tag for entries this handler will handle
   */
  String handlesTag();

  /**
   * Determine whether this handler can handle the data given by this tag.  The
   * tag in an IDF file (or ADF header) is the first token on any given line,
   * and the tag in an SDRF file (or ADF graph part) is the column heading for
   * each tab-separated column.  Pass this tag to the handler to determine
   * whether this handler can handle this line.
   *
   * @param tag the first token on a line, or the column heading, which is the
   *            string describing this line contents
   * @return true if this handler can handle this tag, false otherwise
   */
  boolean canHandle(String tag);

  /**
   * Invoke the current handler.  The handler should have been properly
   * configured with any required variables, but it is left up to specific
   * implementations to determine what is required.  Depending on the handler
   * mode, this will usually simple invoke read() and write() as specified.
   *
   * @throws ParseException            if the handler failed to read data from
   *                                   it's source
   * @throws ObjectConversionException if the handler failed to write or
   *                                   validate appropriate objects
   */
  void handle() throws ParseException, ObjectConversionException;

  /**
   * Accept a visit from a {@link HandlerVisitor}
   *
   * @param visitor the visitor to accept a visit from
   */
  void accept(HandlerVisitor visitor);
}

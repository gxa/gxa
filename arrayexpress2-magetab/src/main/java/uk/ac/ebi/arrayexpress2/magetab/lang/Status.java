package uk.ac.ebi.arrayexpress2.magetab.lang;

/**
 * An enumeration of possible states that a datamodel object can have.  Handlers
 * should apply status updates to indicate the total progress.  This si useful
 * when, for example, certain tasks cannot be completed until the datamodel
 * object acquires a certain status - for example, hibenrate objects cannot be
 * compiled until reading has completed, in most cases.
 *
 * @author Tony Burdett
 * @date 12-Feb-2009
 */
public enum Status {
  /**
   * Designates the status of a task as having failed
   */
  FAILED,
  /**
   * Designates the status of a task as ready to begin
   */
  READY,
  /**
   * Designates the status of a task as currently reading from a file
   */
  READING,
  /**
   * Designates the status of a task as currently compiling parsed data into
   * database model objects
   */
  COMPILING,
  /**
   * Designates the status of a task as finished compiling, being validated
   * against other objects
   */
  VALIDATING,
  /**
   * Designates the status of a task as being stored in the database
   */
  PERSISTING,
  /**
   * Designates the status of a task as finished
   */
  COMPLETE
}

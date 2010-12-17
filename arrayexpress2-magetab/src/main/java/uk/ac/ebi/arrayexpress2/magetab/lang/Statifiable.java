package uk.ac.ebi.arrayexpress2.magetab.lang;

/**
 * This interface forces implementations to have a {@link Status}.
 * Implementations must be thread safe, ensuring that clients can call
 * getStatus() and acquire a snapshot of the objects status at that moment in
 * time.
 * <p/>
 * Statifiable objects should also be {@link Comparable}.  Objects with a higher
 * status (that is, closer to <code>Status.COMPLETE</code>) should be greater
 * than those of lower status.  In addition, this interface defines two
 * additional methods, {@link #ranksAbove(Status)} and {@link
 * #ranksBelow(Status)} that are essentially convenience methods over {@link
 * #compareTo(Object)} that interprets the result to a boolean.
 *
 * @author Tony Burdett
 * @date 09-Feb-2010
 */
public interface Statifiable extends Comparable {
  /**
   * Returns the status of this object
   *
   * @return the status of this object
   */
  Status getStatus();

  /**
   * Returns true if this instance has a higher rank than the {@link Status}
   * parameter passed.
   *
   * @param status the status to compare t
   * @return true if this object has a higher status than the parameter
   *         supplied
   */
  boolean ranksAbove(Status status);

  /**
   * Returns true if this instance has a lower rank than the {@link Status}
   * parameter passed.
   *
   * @param status the status to compare to
   * @return true if this object has a lower status than the parameter supplied
   */
  boolean ranksBelow(Status status);
}

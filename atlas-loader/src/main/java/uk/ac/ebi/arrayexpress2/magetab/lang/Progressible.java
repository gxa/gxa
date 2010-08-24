package uk.ac.ebi.arrayexpress2.magetab.lang;

/**
 * This interface defines objects that have a concept of progress, and can
 * return their current completion percentage.
 * <p/>
 * Implementations must be thread-safe, and should supply implementations of
 * methods to get the current progress and increment progress by a given
 * amount.
 *
 * @author Tony Burdett
 * @date 09-Feb-2010
 */
public interface Progressible {
  /**
   * Gets the current percentage complete of this object, to the nearest whole
   * integer
   *
   * @return the current percentage completion
   */
  int getProgress();

  /**
   * Increment the percentage completion of this object.
   *
   * @param increase the amount to increment the current progress by
   */
  void increaseProgressBy(double increase);
}

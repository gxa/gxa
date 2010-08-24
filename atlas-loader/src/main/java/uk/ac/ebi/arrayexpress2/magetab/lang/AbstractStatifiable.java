package uk.ac.ebi.arrayexpress2.magetab.lang;

/**
 * An abstract state tracker.  State can be updated, and statifiable
 * implementations are comparable; this means a statifiable object can be
 * compared to another object and their respective order determinied.
 *
 * @author Tony Burdett
 * @date 09-Feb-2010
 */
public abstract class AbstractStatifiable implements Statifiable {
  private Status lastStatus = Status.READY;

  /**
   * Returns the status of this object.
   *
   * @return the status of the IDF parse
   */
  public synchronized Status getStatus() {
    return lastStatus;
  }

  /**
   * Explicitly sets the last status of this object
   *
   * @param nextStatus the status to set for this object
   */
  protected synchronized void setStatus(Status nextStatus) {
    lastStatus = nextStatus;
  }

  public boolean ranksAbove(Status status) {
    return getStatus().ordinal() > status.ordinal();
  }

  public boolean ranksBelow(Status status) {
    return getStatus().ordinal() < status.ordinal();
  }

  public int compareTo(Object o) {
    if (o instanceof Statifiable) {
      Statifiable statifiable = (Statifiable) o;
      if (getStatus().equals(statifiable.getStatus())) {
        return 0;
      }
      else {
        return
            getStatus().ordinal() > statifiable.getStatus().ordinal() ? 1 : -1;
      }
    }
    else {
      throw new ClassCastException(
          "Cannot compare object of type " + o.getClass() + " with " +
              Statifiable.class);
    }
  }
}

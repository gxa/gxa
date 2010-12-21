package uk.ac.ebi.arrayexpress2.magetab.lang;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An abstract implementation of a parser progress tracker, that ensures the
 * state of the operation is in line with the state of the constituent tasks.
 * Whenever the state, or progress, of a registered sub-task is updated, the
 * state or progress of the operation as a whole is updated.  This removes the
 * need for IDF< SDRF, or ADF objects to track state or progress explicitly.
 *
 * @author Tony Burdett
 * @date 09-Feb-2010
 */
public abstract class AbstractProgressibleStatifiableFromTasks
    extends AbstractProgressible implements Statifiable {
  private Status[] taskStates;
  private Status lastStatus;

  private static Log log = LogFactory.getLog(AbstractProgressibleStatifiableFromTasks.class);

  public AbstractProgressibleStatifiableFromTasks() {
    taskStates = new Status[]{Status.READY};
    lastStatus = Status.READY;
  }

  /**
   * Set the total number of parsing tasks required to complete parsing the IDF
   * document.  This is used in the progress calculation - i.e. if the IDF
   * contains twenty lines, each line parsed will increment the progress by 5.
   *
   * @param totalTasks the total number of parsing tasks
   */
  public synchronized void setNumberOfTasks(int totalTasks) {
    this.taskStates = new Status[totalTasks];
    for (int i = 0; i < totalTasks; i++) {
      taskStates[i] = Status.READY;
    }
  }

  public synchronized int getNumberOfTasks() {
    return taskStates.length;
  }

  /**
   * Update the status of a task in the current task list.  When parsing, each
   * handler should update it's own status to one of the allowed values, using
   * this method.  The IDF parser then acquires knowledge of the current state
   * of each parser that is currently populating the model.
   *
   * @param taskIndex  the index of the task being updated
   * @param taskStatus the new status
   */
  public synchronized void updateTaskList(int taskIndex, Status taskStatus) {
    taskStates[taskIndex] = taskStatus;
  }

  /**
   * Explicitly sets the last status of this object.  You should not normally
   * use this, unless you have a reason to explicitly set the status of this
   * object to "FAILED".  Setting the status will override the last known
   * status, so that when the next update occurs you may get no notifications,
   * depending on the previous state.
   *
   * @param nextStatus the status to set for this IDF
   */
  protected synchronized void setStatus(Status nextStatus) {
    if (nextStatus == Status.FAILED) {
      log.error("Updating status - parsing failed");
    }
    lastStatus = nextStatus;
    notifyAll();
  }

  /**
   * Returns the status of the IDF parsing operation as a whole.  This is
   * effectively the same as the lowest status parser currently configured.
   *
   * @return the status of the IDF parse
   */
  public synchronized Status getStatus() {
    if (lastStatus == Status.FAILED) {
      notifyAll();
      return lastStatus;
    }

    Status status = Status.COMPLETE;
    int i = 0;
    for (Status taskStatus : taskStates) {
      i++;
      // check the next status - lower than highest point?
      if (taskStatus.ordinal() < status.ordinal()) {
        // if so, revise downwards
        status = taskStatus;
        log.trace("New lowest status found: " + status + ", task index " + i);
      }
    }

    log.trace("Current lowest task status: " + status);
    if (lastStatus != status) {
      log.trace("Previous status: " + status);
      lastStatus = status;
      notifyAll();
    }

    return status;
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

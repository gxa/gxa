package uk.ac.ebi.gxa.tasks;

/**
 * @author pashky
 */
public interface QueuedTask extends Task {
    void setRunMode(TaskRunMode runMode);
    WorkingTask getWorkingTask();
    boolean isBlockedBy(Task otherTask);
}

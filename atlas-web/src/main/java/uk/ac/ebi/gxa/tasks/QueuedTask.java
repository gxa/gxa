package uk.ac.ebi.gxa.tasks;

/**
 * Queued task interface
 *
 * @author pashky
 */
public interface QueuedTask extends Task {
    /**
     * Allows to change task running mode
     * @param runMode new run mode
     */
    void setRunMode(TaskRunMode runMode);

    /**
     * Convert task to working. Does not start it, just creates (or converts) it
     * @return working task reference
     */
    WorkingTask getWorkingTask();

    /**
     * Checks if this task should be blocked by another tasks and wait until its completion
     * @param otherTask other task
     * @return true if should be blocked
     */
    boolean isBlockedBy(Task otherTask);
}

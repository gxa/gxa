package uk.ac.ebi.gxa.tasks;

/**
 * @author pashky
 */
public interface WorkingTaskFactory {
    public WorkingTask createTask(TaskManager queue, TaskSpec taskSpec, TaskRunMode runMode);
    public boolean isForType(String type);
}

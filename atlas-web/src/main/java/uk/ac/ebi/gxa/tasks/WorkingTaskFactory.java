package uk.ac.ebi.gxa.tasks;

import java.util.Collection;

/**
 * @author pashky
 */
public interface WorkingTaskFactory {
    public WorkingTask createTask(TaskManager queue, Task prototype);
    public boolean isForType(TaskSpec taskSpec);
    public boolean isBlockedBy(TaskSpec by);
    public Collection<TaskSpec> autoAddAfter(TaskSpec taskSpec);
}

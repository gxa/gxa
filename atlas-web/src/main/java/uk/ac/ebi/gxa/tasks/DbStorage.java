package uk.ac.ebi.gxa.tasks;

/**
 * @author pashky
 */
public class DbStorage implements PersistentStorage {
    public void writeTaskLog(TaskSpec task, TaskStage stage, TaskStageEvent event, String message) {

    }

    public void updateTaskStage(TaskSpec task, TaskStage stage) {

    }

    public TaskStage getTaskStage(TaskSpec task) {
        return null;        
    }

    public void writeOperationLog(TaskSpec task, TaskRunMode runMode, TaskUser user, TaskOperation operation, String message) {

    }
}

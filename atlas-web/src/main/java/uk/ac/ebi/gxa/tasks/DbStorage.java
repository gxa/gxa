package uk.ac.ebi.gxa.tasks;

import uk.ac.ebi.gxa.dao.AtlasDAO;

import java.util.Map;
import java.util.HashMap;

/**
 * @author pashky
 */
public class DbStorage implements PersistentStorage {
    private AtlasDAO dao;

    Map<TaskSpec, TaskStage> taskStages = new HashMap<TaskSpec, TaskStage>();

    public void setDao(AtlasDAO dao) {
        this.dao = dao;
    }

    public void logTaskStageEvent(TaskSpec task, TaskStage stage, TaskStageEvent event, String message) {

    }

    public synchronized void updateTaskStage(TaskSpec task, TaskStage stage) {
        taskStages.put(task, stage);
    }

    public synchronized TaskStage getTaskStage(TaskSpec task) {
        return taskStages.get(task) != null ? taskStages.get(task) : TaskStage.NONE;
    }

    public void logTaskOperation(TaskSpec task, TaskRunMode runMode, TaskUser user, TaskOperation operation, String message) {

    }
}

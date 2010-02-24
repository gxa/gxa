package uk.ac.ebi.gxa.tasks;

/**
 * @author pashky
 */
public interface Task {
    int getTaskId();
        
    TaskSpec getTaskSpec();

    TaskStage getCurrentStage();

    TaskRunMode getRunMode();
}

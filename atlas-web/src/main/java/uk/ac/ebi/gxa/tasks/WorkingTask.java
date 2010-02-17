package uk.ac.ebi.gxa.tasks;

/**
 * @author pashky
 */
public interface WorkingTask {

    TaskSpec getTaskSpec();

    TaskStage getCurrentStage();

    void start();

    void stop();

}

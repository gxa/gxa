package uk.ac.ebi.gxa.tasks;

/**
 * @author pashky
 */
public interface WorkingTask extends Task {

    void start();

    void stop();

    String getCurrentProgress();

}

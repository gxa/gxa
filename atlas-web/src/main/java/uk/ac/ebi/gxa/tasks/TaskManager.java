package uk.ac.ebi.gxa.tasks;

import uk.ac.ebi.gxa.analytics.generator.AnalyticsGenerator;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGenerator;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author pashky
 */
public class TaskManager implements InitializingBean {
    private static Logger log = LoggerFactory.getLogger(TaskManager.class);

    private AnalyticsGenerator analyticsGenerator;
    private IndexBuilder indexBuilder;
    private NetCDFGenerator netcdfGenerator;
    private PersistentStorage storage;
    private volatile boolean running = true;

    private static List<WorkingTaskFactory> taskFactories = new ArrayList<WorkingTaskFactory>();

    static {
        taskFactories.add(ExperimentTask.FACTORY);
        taskFactories.add(IndexTask.FACTORY);
    }

    private static class QueuedTask {
        final TaskSpec taskSpec;
        final TaskRunMode runMode;

        QueuedTask(TaskSpec taskSpec, TaskRunMode runMode) {
            this.taskSpec = taskSpec;
            this.runMode = runMode;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || taskSpec.equals(((QueuedTask)o).taskSpec);
        }
    }

    private final Queue<QueuedTask> taskQueue = new LinkedList<QueuedTask>();

    private final Collection<WorkingTask> workingTasks = new LinkedList<WorkingTask>();

    public void setStorage(PersistentStorage storage) {
        this.storage = storage;
    }

    AnalyticsGenerator getAnalyticsGenerator() {
        return analyticsGenerator;
    }

    public void setAnalyticsGenerator(AnalyticsGenerator analyticsGenerator) {
        this.analyticsGenerator = analyticsGenerator;
    }

    IndexBuilder getIndexBuilder() {
        return indexBuilder;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
    }

    NetCDFGenerator getNetcdfGenerator() {
        return netcdfGenerator;
    }

    public void setNetcdfGenerator(NetCDFGenerator netcdfGenerator) {
        this.netcdfGenerator = netcdfGenerator;
    }

    public void afterPropertiesSet() throws Exception {
        start();
    }

    public void enqueueTask(TaskSpec taskSpec, TaskRunMode runMode, TaskUser user) {
        synchronized(this) {
            log.info("Queuing task " + taskSpec + " in mode " + runMode + " as user " + user);
            storage.writeOperationLog(taskSpec, runMode, user, TaskOperation.ENQUEUE, "");

            // here go the biznezz rules of task queuing logic
            taskQueue.add(new QueuedTask(taskSpec, runMode));

            if(running && workingTasks.size() == 0) { // TODO: check if we can start straight away
                log.info("We can start right now");
                runNextTask();
            }
        }
    }

    public void cancelTask(TaskSpec taskSpec, TaskUser user) {
        synchronized(this) {
            log.info("Cancelling tasks " + taskSpec + " as user " + user);
            storage.writeOperationLog(taskSpec, null, user, TaskOperation.CANCEL, "");
            for(WorkingTask task : workingTasks)
                if(taskSpec.equals(task.getTaskSpec())) {
                    log.info("It's working now, requesting to stop");
                    task.stop();
                }

            taskQueue.remove(new QueuedTask(taskSpec, null));
        }
    }

    public void start() {
        running = true;
        runNextTask();
    }

    public void pause() {
        running = false;
    }

    public boolean isRunningSomething() {
        synchronized(this) {
            return !workingTasks.isEmpty();
        }
    }

    private void runNextTask() {
        synchronized (this) {
            QueuedTask nextTask = taskQueue.poll();
            if(nextTask == null) {
                log.info("No more tasks to execute");
                return;
            }

            log.info("Task " + nextTask.taskSpec + " is about to start in " + nextTask.runMode + " mode");

            for(WorkingTaskFactory factory : taskFactories) {
                if(factory.isForType(nextTask.taskSpec.getType())) {
                    WorkingTask workingTask = factory.createTask(this, nextTask.taskSpec, nextTask.runMode);
                    workingTasks.add(workingTask);
                    workingTask.start();
                    return;
                }
            }
            log.error("Can't find factory for task " + nextTask.taskSpec);
        }
    }

    void notifyTaskFinished(WorkingTask task) {
        synchronized (this) {
            log.info("Task " + task.getTaskSpec() + " finished at stage " + task.getCurrentStage());
            workingTasks.remove(task);
        }
        if(running)
            runNextTask();
    }

    void updateTaskStage(TaskSpec taskSpec, TaskStage stage) {
        storage.updateTaskStage(taskSpec, stage);
    }

    TaskStage getTaskStage(TaskSpec taskSpec) {
        return storage.getTaskStage(taskSpec);
    }

    void writeTaskLog(TaskSpec taskSpec, TaskStage stage, TaskStageEvent event, String message) {
        storage.writeTaskLog(taskSpec, stage, event, message);
    }

}

package uk.ac.ebi.gxa.tasks;

import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.Collection;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

/**
 * @author pashky
 */
public class IndexTask implements WorkingTask {
    public static final String TYPE = "index";
    public static final TaskStage INDEX_STAGE = TaskStage.valueOf("INDEX"); // we have only one non-done stage here
    private final TaskSpec spec;
    private final TaskManager queue;
    private final TaskRunMode runMode;
    private volatile TaskStage currentStage;
    private final int taskId;

    private IndexTask(final TaskManager queue, final int taskId, final TaskSpec spec, final TaskRunMode runMode) {
        this.taskId = taskId;
        this.spec = spec;
        this.queue = queue;
        this.runMode = runMode;
        this.currentStage = queue.getTaskStage(spec);
    }

    public TaskSpec getTaskSpec() {
        return spec;
    }

    public TaskStage getCurrentStage() {
        return currentStage;
    }

    public TaskRunMode getRunMode() {
        return runMode;
    }

    public int getTaskId() {
        return taskId;
    }

    public void start() {
        if(runMode == TaskRunMode.CONTINUE && TaskStage.DONE.equals(currentStage)) {
            queue.notifyTaskFinished(this);
            return;
        }

        Thread thread = new Thread(new Runnable() {
            public void run() {
                queue.updateTaskStage(spec, INDEX_STAGE);
                queue.writeTaskLog(spec, INDEX_STAGE, TaskStageEvent.STARTED, "");
                final AtomicReference<IndexBuilderEvent> result = new AtomicReference<IndexBuilderEvent>(null);
                queue.getIndexBuilder().buildIndex(new IndexBuilderListener() {
                    public void buildSuccess(IndexBuilderEvent event) {
                        synchronized (IndexTask.this) {
                            result.set(event);
                            IndexTask.this.notifyAll();
                        }
                    }

                    public void buildError(IndexBuilderEvent event) {
                        synchronized (IndexTask.this) {
                            result.set(event);
                            IndexTask.this.notifyAll();
                        }
                    }
                });

                synchronized (IndexTask.this) {
                    while(result.get() == null) {
                        try {
                            IndexTask.this.wait();
                        } catch(InterruptedException e) {
                            // continue
                        }
                    }
                }

                if(result.get().getStatus() == IndexBuilderEvent.Status.SUCCESS) {
                    queue.writeTaskLog(spec, INDEX_STAGE, TaskStageEvent.FINISHED, "");
                    queue.updateTaskStage(spec, TaskStage.DONE);
                } else {
                    queue.writeTaskLog(spec, INDEX_STAGE, TaskStageEvent.FAILED, StringUtils.join(result.get().getErrors(), '\n'));
                }
                currentStage = TaskStage.DONE;
                queue.notifyTaskFinished(IndexTask.this); // it's waiting for this
            }
        });
        thread.setName("IndexTaskThread-" + getTaskSpec() + "-" + getTaskId());
        thread.start();
    }

    public void stop() {
        // can't stop this task as there's no stages and no control of index builder when it's running
    }

    public static final WorkingTaskFactory FACTORY = new WorkingTaskFactory() {
        public WorkingTask createTask(TaskManager queue, Task prototype) {
            return new IndexTask(queue, prototype.getTaskId(), prototype.getTaskSpec(), prototype.getRunMode());
        }

        public boolean isForType(TaskSpec taskSpec) {
            return TYPE.equals(taskSpec.getType());
        }

        public boolean isBlockedBy(TaskSpec by) {
            return ExperimentTask.TYPE.equals(by.getType());
        }

        public Collection<TaskSpec> autoAddAfter(TaskSpec taskSpec) {
            return new ArrayList<TaskSpec>();
        }
    };
}

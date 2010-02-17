package uk.ac.ebi.gxa.tasks;

import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;

/**
 * @author pashky
 */
public class IndexTask implements WorkingTask {
    private static final String TYPE = "index";
    private static final TaskStage INDEX = TaskStage.valueOf("INDEX"); // we have only one non-done stage here
    private final TaskSpec spec;
    private final TaskManager queue;
    private final TaskRunMode runMode;
    private volatile TaskStage currentStage = INDEX;

    private IndexTask(final TaskManager queue, final TaskSpec spec, final TaskRunMode runMode) {
        this.spec = spec;
        this.queue = queue;
        this.runMode = runMode;
    }

    public TaskSpec getTaskSpec() {
        return spec;
    }

    public TaskStage getCurrentStage() {
        return currentStage;
    }

    public void start() {
        if(runMode == TaskRunMode.CONTINUE && TaskStage.DONE.equals(queue.getTaskStage(spec))) {
            queue.notifyTaskFinished(this);
        }

        Thread thread = new Thread(new Runnable() {
            public void run() {
                queue.updateTaskStage(spec, INDEX);
                queue.writeTaskLog(spec, INDEX, TaskStageEvent.STARTED, "");
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
                    queue.writeTaskLog(spec, INDEX, TaskStageEvent.FINISHED, "");
                    queue.updateTaskStage(spec, TaskStage.DONE);
                } else {
                    queue.writeTaskLog(spec, INDEX, TaskStageEvent.FAILED, StringUtils.join(result.get().getErrors(), '\n'));
                }
                queue.notifyTaskFinished(IndexTask.this); // it's waiting for this
                currentStage = TaskStage.DONE;
            }
        });
        thread.setName(getClass().getSimpleName());
        thread.start();
    }

    public void stop() {
        // can't stop this task as there's no stages and no control of index builder when it's running
    }

    public static final WorkingTaskFactory FACTORY = new WorkingTaskFactory() {
        public WorkingTask createTask(TaskManager queue, TaskSpec spec, TaskRunMode runMode) {
            return new IndexTask(queue, spec, runMode);
        }

        public boolean isForType(String type) {
            return TYPE.equals(type);
        }
    };
}

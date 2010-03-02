package uk.ac.ebi.gxa.tasks;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderEvent;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author pashky
 */
public class LoaderTask implements WorkingTask {
    public static final String TYPE_EXPERIMENT = "loadexperiment";
    public static final String TYPE_ARRAYDESIGN = "loadarraydesign";
    
    public static final TaskStage STAGE = TaskStage.valueOf("LOAD"); // we have only one non-done stage here
    private final TaskSpec spec;
    private final TaskManager queue;
    private final TaskRunMode runMode;
    private volatile TaskStage currentStage;
    private final int taskId;
    private volatile String currentProgress = "";

    private LoaderTask(final TaskManager queue, final int taskId, final TaskSpec spec, final TaskRunMode runMode) {
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

    private static class TaskInternalError extends Exception { }

    public void start() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                if(runMode == TaskRunMode.CONTINUE && TaskStage.DONE.equals(currentStage)) {
                    queue.notifyTaskFinished(LoaderTask.this);
                    return;
                }

                queue.updateTaskStage(spec, STAGE);
                queue.writeTaskLog(spec, STAGE, TaskStageEvent.STARTED, "");
                final AtomicReference<AtlasLoaderEvent> result = new AtomicReference<AtlasLoaderEvent>(null);

                try {
                    AtlasLoaderListener listener = new AtlasLoaderListener() {
                        public void loadSuccess(AtlasLoaderEvent event) {
                            synchronized (LoaderTask.this) {
                                result.set(event);
                                LoaderTask.this.notifyAll();
                            }
                        }

                        public void loadError(AtlasLoaderEvent event) {
                            synchronized (LoaderTask.this) {
                                result.set(event);
                                LoaderTask.this.notifyAll();
                            }
                        }

                        public void loadProgress(int progress) {
                            currentProgress = progress + "%";
                        }
                    };
                    if(TYPE_EXPERIMENT.equals(spec.getType()))
                        queue.getLoader().loadExperiment(new URL(spec.getAccession()), listener);
                    else if(TYPE_ARRAYDESIGN.equals(spec.getType()))
                        queue.getLoader().loadArrayDesign(new URL(spec.getAccession()), listener);
                    else
                        throw new TaskInternalError();

                    synchronized (LoaderTask.this) {
                        while(result.get() == null) {
                            try {
                                LoaderTask.this.wait();
                            } catch(InterruptedException e) {
                                // continue
                            }
                        }
                    }

                    if(result.get().getStatus() == AtlasLoaderEvent.Status.SUCCESS) {
                        for(String accession : result.get().getAccessions()) {
                            if(TYPE_EXPERIMENT.equals(spec.getType())) {
                                queue.enqueueTask(
                                        new TaskSpec(ExperimentTask.TYPE, accession),
                                        TaskRunMode.RESTART,
                                        new TaskUser("LoaderTask"),
                                        true
                                );
                            } else if(TYPE_ARRAYDESIGN.equals(spec.getType())) {
                                // array design loaded. index task should be already queued. 
                            } else
                                throw new TaskInternalError();
                        }

                        queue.writeTaskLog(spec, STAGE, TaskStageEvent.FINISHED, "");
                        queue.updateTaskStage(spec, TaskStage.DONE);
                        currentStage = TaskStage.DONE;
                    } else {
                        queue.writeTaskLog(spec, STAGE, TaskStageEvent.FAILED, StringUtils.join(result.get().getErrors(), '\n'));
                    }

                } catch(MalformedURLException e) {
                    queue.writeTaskLog(spec, STAGE, TaskStageEvent.FAILED, "Invalid URL " + spec.getAccession());
                } catch(TaskInternalError e) {
                    queue.writeTaskLog(spec, STAGE, TaskStageEvent.FAILED, "Impossible happened");
                }

                queue.notifyTaskFinished(LoaderTask.this); // it's waiting for this
            }
        });
        thread.setName("LoaderTaskThread-" + getTaskSpec() + "-" + getTaskId());
        thread.start();
    }

    public void stop() {
        // can't stop this task as there's no stages and no control of index builder when it's running
    }

    public String getCurrentProgress() {
        return currentProgress;
    }

    public static final WorkingTaskFactory FACTORY = new WorkingTaskFactory() {
        public WorkingTask createTask(TaskManager queue, Task prototype) {
            return new LoaderTask(queue, prototype.getTaskId(), prototype.getTaskSpec(), prototype.getRunMode());
        }

        public boolean isForType(TaskSpec taskSpec) {
            return TYPE_EXPERIMENT.equals(taskSpec.getType()) || TYPE_ARRAYDESIGN.equals(taskSpec.getType());
        }

        public boolean isBlockedBy(TaskSpec by) {
            return isForType(by);
        }

        public Collection<TaskSpec> autoAddAfter(TaskSpec taskSpec) {
            if(TYPE_ARRAYDESIGN.equals(taskSpec.getType())) 
                return Collections.singletonList(new TaskSpec(IndexTask.TYPE, ""));
            else
                return new ArrayList<TaskSpec>(0);
        }
    };
    
}

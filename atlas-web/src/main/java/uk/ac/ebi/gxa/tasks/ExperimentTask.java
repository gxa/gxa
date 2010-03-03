/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.tasks;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGenerationEvent;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGenerationEvent;
import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGeneratorListener;

import java.util.concurrent.atomic.AtomicReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;

/**
 * @author pashky
 */
public class ExperimentTask implements WorkingTask {
    private static Logger log = LoggerFactory.getLogger(ExperimentTask.class);
    public static final String TYPE = "experiment";

    private final TaskManager queue;
    private final TaskRunMode runMode;
    private final TaskSpec taskSpec;
    private volatile boolean stop;
    private volatile TaskStage currentStage;
    private final int taskId;
    private volatile String currentProgress = "";

    private enum Stage {
        NETCDF {
            public boolean run(final ExperimentTask task) {
                final AtomicReference<NetCDFGenerationEvent> result = new AtomicReference<NetCDFGenerationEvent>(null);
                task.queue.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.STARTED, "");
                task.queue.getNetcdfGenerator().generateNetCDFsForExperiment(task.taskSpec.getAccession(),
                        new NetCDFGeneratorListener() {
                            public void buildSuccess(NetCDFGenerationEvent event) {
                               synchronized (task) {
                                    result.set(event);
                                    task.notifyAll();
                                }
                            }

                            public void buildError(NetCDFGenerationEvent event) {
                                synchronized (task) {
                                    result.set(event);
                                    task.notifyAll();
                                }
                            }

                            public void buildProgress(String progressStatus) {
                                if(progressStatus.length() > 0)
                                    log.info(progressStatus);
                                task.currentProgress = progressStatus;
                            }
                        });
                synchronized (task) {
                    while(result.get() == null)
                        try {
                            task.wait();
                        } catch (InterruptedException e) {
                            // skip
                        }
                }
                if(result.get().getStatus() == NetCDFGenerationEvent.Status.SUCCESS) {
                    task.queue.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.FINISHED, "");
                    return true;
                } else {
                    for(Throwable e : result.get().getErrors()) {
                        log.error("Task failed because of:", e);
                    }
                    task.queue.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.FAILED, StringUtils.join(result.get().getErrors(), '\n'));
                    return false;
                }
            }
        },

        ANALYTICS {
            public boolean run(final ExperimentTask task) {
                final AtomicReference<AnalyticsGenerationEvent> result = new AtomicReference<AnalyticsGenerationEvent>(null);
                task.queue.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.STARTED, "");
                task.queue.getAnalyticsGenerator().generateAnalyticsForExperiment(task.taskSpec.getAccession(),
                        new AnalyticsGeneratorListener() {
                            public void buildSuccess(AnalyticsGenerationEvent event) {
                                synchronized (task) {
                                    result.set(event);
                                    task.notifyAll();
                                }
                            }

                            public void buildError(AnalyticsGenerationEvent event) {
                                synchronized (task) {
                                    result.set(event);
                                    task.notifyAll();
                                }
                            }

                            public void buildProgress(String progressStatus) {
                                if(progressStatus.length() > 0)
                                    log.info(progressStatus);
                                task.currentProgress = progressStatus;
                            }
                        });
                synchronized (task) {
                    while(result.get() == null)
                        try {
                            task.wait();
                        } catch (InterruptedException e) {
                            // skip
                        }
                }
                if(result.get().getStatus() == AnalyticsGenerationEvent.Status.SUCCESS) {
                    task.queue.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.FINISHED, "Successfully");
                    return true;
                } else {
                    for(Throwable e : result.get().getErrors()) {
                        log.error("Task failed because of:", e);
                    }
                    task.queue.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.FAILED, StringUtils.join(result.get().getErrors(), '\n'));
                    return false;
                }
            }
        },

        DONE {
            public boolean run(ExperimentTask task) {
                return true; // what's done is done, do nothing
            }
        };

        abstract boolean run(ExperimentTask task);
        TaskStage stage() { return TaskStage.valueOf(this); }
    }

    public void start() {

        final Stage fromStage;
        if(runMode == TaskRunMode.CONTINUE) {
            if(TaskStage.DONE.equals(currentStage)) {
                new Thread() { // TODO: that's awful, but let's leave for the moment 
                    public void run() {
                        queue.notifyTaskFinished(ExperimentTask.this);
                    }
                }.start();
                return; // do nothing, "continue" fired on finished task by mistake
            }
            if(TaskStage.NONE.equals(currentStage))
                fromStage = Stage.values()[0]; // continue from nothing = start from scratch
            else
                fromStage = Stage.valueOf(currentStage.getStage()); // current status = stage, which is to be completed
        } else
            fromStage = Stage.values()[0];

        stop = false;
        if(fromStage == Stage.DONE)
            return;

        Thread thread = new Thread(new Runnable() {
            public void run() {
                for(int stageId = fromStage.ordinal(); stageId < Stage.values().length; ++stageId) {
                    Stage stage = Stage.values()[stageId];
                    currentStage = TaskStage.valueOf(stage);
                    ExperimentTask.this.queue.updateTaskStage(getTaskSpec(), currentStage); // here we are, setting stage which is about to start
                    if(stop) {
                        // we've been stopped in the meanwhile, so do not continue and generate a "fail" event
                        queue.writeTaskLog(getTaskSpec(), currentStage, TaskStageEvent.STOPPED, "Stopped by user request");
                        break;
                    }

                    boolean doContinue = stage.run(ExperimentTask.this);
                    if(!doContinue)
                        break;
                }
                if(TaskStage.DONE.equals(currentStage)) {
                    // reset index to "dirty" stage
                    queue.updateTaskStage(new TaskSpec(IndexTask.TYPE, ""), IndexTask.INDEX_STAGE);
                }
                queue.notifyTaskFinished(ExperimentTask.this); // it's waiting for this
            }
        });
        thread.setName("ExperimentTaskThread-" + getTaskSpec() + "-" + getTaskId());
        thread.start();
    }

    private ExperimentTask(final TaskManager queue, final int taskId, final TaskSpec taskSpec, final TaskRunMode runMode) {
        this.queue = queue;
        this.taskId = taskId;
        this.taskSpec = taskSpec;
        this.runMode = runMode;
        this.currentStage = queue.getTaskStage(getTaskSpec());
    }

    public TaskSpec getTaskSpec() {
        return taskSpec;
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

    public void stop() {
        stop = true;
    }

    public String getCurrentProgress() {
        return currentProgress;
    }

    public static final WorkingTaskFactory FACTORY = new WorkingTaskFactory() {
        public WorkingTask createTask(TaskManager queue, Task prototype) {
            return new ExperimentTask(queue, prototype.getTaskId(), prototype.getTaskSpec(), prototype.getRunMode());
        }

        public boolean isForType(TaskSpec taskSpec) {
            return TYPE.equals(taskSpec.getType());
        }

        public boolean isBlockedBy(TaskSpec by) {
            return Arrays.asList(
                    LoaderTask.TYPE_EXPERIMENT
            ).contains(by.getType());
        }

        public Collection<TaskSpec> autoAddAfter(TaskSpec taskSpec) {
            return Collections.singletonList(new TaskSpec(IndexTask.TYPE, ""));
        }
    };


}

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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author pashky
 */
public class ExperimentTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(ExperimentTask.class);
    
    public static final String TYPE = "experiment";

    private volatile boolean stop;

    public enum Stage {
        NETCDF {
            public boolean run(final ExperimentTask task) {
                final AtomicReference<NetCDFGenerationEvent> result = new AtomicReference<NetCDFGenerationEvent>(null);
                task.taskMan.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.STARTED, "");
                task.taskMan.getNetcdfGenerator().generateNetCDFsForExperiment(task.getTaskSpec().getAccession(),
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
                    task.taskMan.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.FINISHED, "");
                    return true;
                } else {
                    for(Throwable e : result.get().getErrors()) {
                        log.error("Task failed because of:", e);
                    }
                    task.taskMan.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.FAILED, StringUtils.join(result.get().getErrors(), '\n'));
                    return false;
                }
            }
        },

        ANALYTICS {
            public boolean run(final ExperimentTask task) {
                final AtomicReference<AnalyticsGenerationEvent> result = new AtomicReference<AnalyticsGenerationEvent>(null);
                task.taskMan.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.STARTED, "");
                task.taskMan.getAnalyticsGenerator().generateAnalyticsForExperiment(task.getTaskSpec().getAccession(),
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
                    task.taskMan.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.FINISHED, "Successfully");
                    return true;
                } else {
                    for(Throwable e : result.get().getErrors()) {
                        log.error("Task failed because of:", e);
                    }
                    task.taskMan.writeTaskLog(task.getTaskSpec(), stage(), TaskStageEvent.FAILED, StringUtils.join(result.get().getErrors(), '\n'));
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

        if(getRunMode() == TaskRunMode.CONTINUE) {
            if(TaskStage.NONE.equals(currentStage))
                fromStage = Stage.values()[0]; // continue from nothing = start from scratch
            else
                fromStage = Stage.valueOf(currentStage.getStage()); // current status = stage, which is to be completed
        } else
            fromStage = Stage.values()[0];

        stop = false;

        Thread thread = new Thread(new Runnable() {
            public void run() {
                if(fromStage != Stage.DONE) {
                    for(int stageId = fromStage.ordinal(); stageId < Stage.values().length; ++stageId) {
                        Stage stage = Stage.values()[stageId];
                        ExperimentTask.this.taskMan.updateTaskStage(getTaskSpec(), currentStage = TaskStage.valueOf(stage)); // here we are, setting stage which is about to start
                        if(stop) {
                            // we've been stopped in the meanwhile, so do not continue and generate a "fail" event
                            taskMan.writeTaskLog(getTaskSpec(), getCurrentStage(), TaskStageEvent.STOPPED, "Stopped by user request");
                            break;
                        }

                        boolean doContinue = stage.run(ExperimentTask.this);
                        if(!doContinue)
                            break;
                    }
                    if(TaskStage.DONE.equals(getCurrentStage())) {
                        // reset index to "dirty" stage
                        TaskSpec indexTask = new TaskSpec(IndexTask.TYPE, "");
                        taskMan.updateTaskStage(indexTask, IndexTask.STAGE);
                        if(isRunningAutoDependencies()) {
                            taskMan.enqueueTask(indexTask, TaskRunMode.CONTINUE, getUser(), true,
                                    "Automatically added by experiment " + getTaskSpec().getAccession() + " processing task");
                        }
                    }
                }
                taskMan.notifyTaskFinished(ExperimentTask.this);
            }
        });

        thread.setName("ExperimentTaskThread-" + getTaskSpec() + "-" + getTaskId());
        thread.start();
    }

    private ExperimentTask(final TaskManager queue, final Task prototype) {
        super(queue, prototype);
    }

    public void stop() {
        stop = true;
    }

    public static final WorkingTaskFactory FACTORY = new WorkingTaskFactory() {
        public WorkingTask createTask(TaskManager queue, Task prototype) {
            return new ExperimentTask(queue, prototype);
        }

        public boolean isForType(TaskSpec taskSpec) {
            return TYPE.equals(taskSpec.getType());
        }

        public boolean isBlockedBy(TaskSpec what, TaskSpec by) {
            return Arrays.asList(
                    LoaderTask.TYPE_EXPERIMENT
            ).contains(by.getType());
        }
    };


}

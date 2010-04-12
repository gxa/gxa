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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author pashky
 */
public class AnalyticsTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(AnalyticsTask.class);
    
    public static final String TYPE = "analytics";

    private boolean stop;

    public void start() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                if(getRunMode() == TaskRunMode.CONTINUE && TaskStatus.DONE.equals(getCurrentStatus())) {
                    taskMan.writeTaskLog(AnalyticsTask.this, TaskEvent.SKIPPED, "");
                    taskMan.notifyTaskFinished(AnalyticsTask.this);
                    return;
                }

                stop = false;
                final AtomicReference<AnalyticsGenerationEvent> result = new AtomicReference<AnalyticsGenerationEvent>(null);
                taskMan.updateTaskStage(getTaskSpec(), TaskStatus.INCOMPLETE);
                taskMan.writeTaskLog(AnalyticsTask.this, TaskEvent.STARTED, "");
                taskMan.getAnalyticsGenerator().generateAnalyticsForExperiment(getTaskSpec().getAccession(),
                        new AnalyticsGeneratorListener() {
                            public void buildSuccess(AnalyticsGenerationEvent event) {
                                synchronized (AnalyticsTask.this) {
                                    result.set(event);
                                    AnalyticsTask.this.notifyAll();
                                }
                            }

                            public void buildError(AnalyticsGenerationEvent event) {
                                synchronized (AnalyticsTask.this) {
                                    result.set(event);
                                    AnalyticsTask.this.notifyAll();
                                }
                            }

                            public void buildProgress(String progressStatus) {
                                if(progressStatus.length() > 0)
                                    log.info(progressStatus);
                                currentProgress = progressStatus;
                            }
                        });
                synchronized (AnalyticsTask.this) {
                    while(result.get() == null)
                        try {
                            AnalyticsTask.this.wait();
                        } catch (InterruptedException e) {
                            // skip
                        }
                }
                if(result.get().getStatus() == AnalyticsGenerationEvent.Status.SUCCESS) {
                    taskMan.writeTaskLog(AnalyticsTask.this, TaskEvent.FINISHED, "Successfully");
                    taskMan.updateTaskStage(getTaskSpec(), TaskStatus.DONE);

                    TaskSpec indexTask = new TaskSpec(IndexTask.TYPE, "");
                    taskMan.updateTaskStage(indexTask, TaskStatus.NONE);
                    if(!stop && isRunningAutoDependencies()) {
                        taskMan.scheduleTask(AnalyticsTask.this, indexTask, TaskRunMode.CONTINUE, getUser(), true,
                                "Automatically added by experiment " + getTaskSpec().getAccession() + " processing task");
                    }

                } else {
                    for(Throwable e : result.get().getErrors()) {
                        log.error("Task failed because of:", e);
                    }
                    taskMan.writeTaskLog(AnalyticsTask.this, TaskEvent.FAILED, StringUtils.join(result.get().getErrors(), '\n'));
                }
                taskMan.notifyTaskFinished(AnalyticsTask.this);
            }
        });

        thread.setName("AnalyticsTaskThread-" + getTaskSpec() + "-" + getTaskId());
        thread.start();
    }

    private AnalyticsTask(final TaskManager queue, final Task prototype) {
        super(queue, prototype);
        taskMan.addTaskTag(AnalyticsTask.this, TaskTagType.EXPERIMENT, getTaskSpec().getAccession());
    }

    public void stop() {
        stop = true;
    }

    public static final WorkingTaskFactory FACTORY = new WorkingTaskFactory() {
        public WorkingTask createTask(TaskManager queue, Task prototype) {
            return new AnalyticsTask(queue, prototype);
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

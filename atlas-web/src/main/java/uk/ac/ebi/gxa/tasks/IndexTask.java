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
 * http://ostolop.github.com/gxa/
 */

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
    private volatile String currentProgress = "";

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
        Thread thread = new Thread(new Runnable() {
            public void run() {
                if(runMode == TaskRunMode.CONTINUE && TaskStage.DONE.equals(currentStage)) {
                    queue.notifyTaskFinished(IndexTask.this);
                    return;
                }

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

                    public void buildProgress(String progressStatus) {
                        currentProgress = progressStatus;
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

    public String getCurrentProgress() {
        return currentProgress;
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

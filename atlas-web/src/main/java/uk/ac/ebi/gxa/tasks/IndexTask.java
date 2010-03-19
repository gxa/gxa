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
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author pashky
 */
public class IndexTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(IndexTask.class);
    
    public static final String TYPE = "index";
    public static final TaskStage STAGE = TaskStage.valueOf("INDEX"); // we have only one non-done stage here

    public void start() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                if(getRunMode() == TaskRunMode.CONTINUE && TaskStage.DONE.equals(currentStage)) {
                    taskMan.notifyTaskFinished(IndexTask.this);
                    return;
                }

                taskMan.updateTaskStage(getTaskSpec(), STAGE);
                taskMan.writeTaskLog(getTaskSpec(), STAGE, TaskStageEvent.STARTED, "");
                final AtomicReference<IndexBuilderEvent> result = new AtomicReference<IndexBuilderEvent>(null);
                taskMan.getIndexBuilder().buildIndex(new IndexBuilderListener() {
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
                    taskMan.writeTaskLog(getTaskSpec(), STAGE, TaskStageEvent.FINISHED, "");
                    taskMan.updateTaskStage(getTaskSpec(), TaskStage.DONE);
                    currentStage = TaskStage.DONE;
                } else {
                    for(Throwable e : result.get().getErrors()) {
                        log.error("Task failed because of:", e);
                    }
                    taskMan.writeTaskLog(getTaskSpec(), STAGE, TaskStageEvent.FAILED, StringUtils.join(result.get().getErrors(), '\n'));
                }
                taskMan.notifyTaskFinished(IndexTask.this); // it's waiting for this
            }
        });
        thread.setName("IndexTaskThread-" + getTaskSpec() + "-" + getTaskId());
        thread.start();
    }

    public void stop() {
        // can't stop this task as there's no stages and no control of index builder when it's running
    }

    private IndexTask(final TaskManager queue, final Task prototype) {
        super(queue, prototype);
    }

    public static final WorkingTaskFactory FACTORY = new WorkingTaskFactory() {
        public WorkingTask createTask(TaskManager queue, Task prototype) {
            return new IndexTask(queue, prototype);
        }

        public boolean isForType(TaskSpec taskSpec) {
            return TYPE.equals(taskSpec.getType());
        }

        public boolean isBlockedBy(TaskSpec what, TaskSpec by) {
            return Arrays.asList(
                    ExperimentTask.TYPE,
                    LoaderTask.TYPE_EXPERIMENT,
                    LoaderTask.TYPE_ARRAYDESIGN
            ).contains(by.getType());
        }
    };
}

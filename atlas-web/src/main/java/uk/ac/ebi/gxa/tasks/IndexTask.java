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

/**
 * @author pashky
 */
public class IndexTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(IndexTask.class);
    
    public static final String TYPE = "index";

    public void start() {
        if(nothingToDo())
            return;

        startTimer();
        taskMan.updateTaskStage(getTaskSpec(), TaskStatus.INCOMPLETE);
        taskMan.writeTaskLog(IndexTask.this, TaskEvent.STARTED, "");
        try {
            taskMan.getIndexBuilder().buildIndex(new IndexBuilderListener() {
                public void buildSuccess(IndexBuilderEvent event) {
                    taskMan.writeTaskLog(IndexTask.this, TaskEvent.FINISHED, "");
                    taskMan.updateTaskStage(getTaskSpec(), TaskStatus.DONE);
                    taskMan.notifyTaskFinished(IndexTask.this); // it's waiting for this
                }

                public void buildError(IndexBuilderEvent event) {
                    for(Throwable e : event.getErrors()) {
                        log.error("Task failed because of:", e);
                    }
                    taskMan.writeTaskLog(IndexTask.this, TaskEvent.FAILED, StringUtils.join(event.getErrors(), '\n'));
                    taskMan.notifyTaskFinished(IndexTask.this); // it's waiting for this
                }

                public void buildProgress(String progressStatus) {
                    currentProgress = progressStatus;
                }
            });
        } catch(Throwable e) {
            taskMan.writeTaskLog(IndexTask.this, TaskEvent.FAILED, e.toString());
            taskMan.notifyTaskFinished(IndexTask.this); // it's waiting for this
        }
    }

    public void stop() {
        // can't stop this task as there's no stages and no control of index builder when it's running
    }

    public IndexTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
        super(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
    }

    public boolean isBlockedBy(Task by) {
        return Arrays.asList(
                AnalyticsTask.TYPE,
                LoaderTask.TYPE_EXPERIMENT,
                LoaderTask.TYPE_ARRAYDESIGN
        ).contains(by.getTaskSpec().getType());
    }
    
    public static final TaskFactory FACTORY = new TaskFactory() {
        public QueuedTask createTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
            return new IndexTask(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        }

        public boolean isFor(TaskSpec taskSpec) {
            return TYPE.equals(taskSpec.getType());
        }

    };
}

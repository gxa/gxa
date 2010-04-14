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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.loader.AtlasUnloaderException;

/**
 * Unload experiment task
 * @author pashky
 */
public class UnloadExperimentTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(UnloadExperimentTask.class);

    public static final String TYPE = "unloadexperiment";

    private boolean stop = false;

    public void start() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                startTimer();
                taskMan.updateTaskStage(getTaskSpec(), TaskStatus.INCOMPLETE);
                taskMan.writeTaskLog(UnloadExperimentTask.this, TaskEvent.STARTED, "");

                try {
                    currentProgress = "Unloading...";
                    stop = false;

                    log.info("Unloading experiment " + getTaskSpec().getAccession());
                    taskMan.getLoader().unloadExperiment(getTaskSpec().getAccession());
                    log.info("Unloading experiment " + getTaskSpec().getAccession() + " - done");

                    taskMan.writeTaskLog(UnloadExperimentTask.this, TaskEvent.FINISHED, "");
                    taskMan.updateTaskStage(getTaskSpec(), TaskStatus.DONE);

                    TaskSpec experimentTask = new TaskSpec(AnalyticsTask.TYPE, getTaskSpec().getAccession());
                    taskMan.updateTaskStage(experimentTask, TaskStatus.NONE);

                    TaskSpec indexTask = new TaskSpec(IndexTask.TYPE, "");
                    taskMan.updateTaskStage(indexTask, TaskStatus.INCOMPLETE);
                    if(!stop && isRunningAutoDependencies()) {
                        taskMan.scheduleTask(UnloadExperimentTask.this, indexTask, TaskRunMode.CONTINUE, getUser(), true,
                                "Automatically added by unload of experiment " + getTaskSpec().getAccession());
                    }
                } catch(AtlasUnloaderException e) {
                    log.error("Unloading experiment " + getTaskSpec().getAccession() + " - failed", e);
                    taskMan.writeTaskLog(UnloadExperimentTask.this, TaskEvent.FAILED, e.toString());
                }

                taskMan.notifyTaskFinished(UnloadExperimentTask.this); // it's waiting for this
            }
        });
        thread.setName("UnloadThread-" + getTaskSpec() + "-" + getTaskId());
        thread.start();

    }

    public void stop() {
        stop = true;
    }

    public UnloadExperimentTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
        super(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        taskMan.addTaskTag(this, TaskTagType.EXPERIMENT, getTaskSpec().getAccession());
    }

    public boolean isBlockedBy(Task by) {
        return TYPE.equals(by.getTaskSpec().getType())
                ||
                (by.getTaskSpec().getType().equals(AnalyticsTask.TYPE)
                        && getTaskSpec().getAccession().equals(by.getTaskSpec().getAccession()));
    }

    public static final TaskFactory FACTORY = new TaskFactory() {
        public QueuedTask createTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
            return new UnloadExperimentTask(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        }

        public boolean isFor(TaskSpec taskSpec) {
            return TYPE.equals(taskSpec.getType());
        }
    };

}

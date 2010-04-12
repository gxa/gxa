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

    boolean stop;

    public void start() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
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

    public UnloadExperimentTask(TaskManager taskMan, Task prototype) {
        super(taskMan, prototype);
        taskMan.addTaskTag(this, TaskTagType.EXPERIMENT, getTaskSpec().getAccession());
    }

    public static final WorkingTaskFactory FACTORY = new WorkingTaskFactory() {
        public WorkingTask createTask(TaskManager queue, Task prototype) {
            return new UnloadExperimentTask(queue, prototype);
        }

        public boolean isForType(TaskSpec taskSpec) {
            return TYPE.equals(taskSpec.getType());
        }

        public boolean isBlockedBy(TaskSpec what, TaskSpec by) {
            return isForType(by) || (by.getType().equals(AnalyticsTask.TYPE) && what.getAccession().equals(by.getAccession()));
        }
    };

}

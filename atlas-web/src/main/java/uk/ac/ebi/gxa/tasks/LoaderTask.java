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
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderEvent;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author pashky
 */
public class LoaderTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(LoaderTask.class);

    public static final String TYPE_EXPERIMENT = "loadexperiment";
    public static final String TYPE_ARRAYDESIGN = "loadarraydesign";
    public static final String TYPE_UPDATEEXPERIMENT = "updateexperiment";

    private volatile boolean stop = false;

    public void start() {
        if(nothingToDo())
            return;

        startTimer();
        taskMan.updateTaskStage(getTaskSpec(), TaskStatus.INCOMPLETE);
        taskMan.writeTaskLog(LoaderTask.this, TaskEvent.STARTED, "");

        AtlasLoaderListener listener = new AtlasLoaderListener() {
            public void loadSuccess(AtlasLoaderEvent event) {
                taskMan.writeTaskLog(LoaderTask.this, TaskEvent.FINISHED, "");
                taskMan.updateTaskStage(getTaskSpec(), TaskStatus.DONE);

                for(String accession : event.getAccessions()) {
                    if(TYPE_EXPERIMENT.equals(getTaskSpec().getType()) ||
                            TYPE_UPDATEEXPERIMENT.equals(getTaskSpec().getType())) {
                        taskMan.addTaskTag(LoaderTask.this, TaskTagType.EXPERIMENT, accession);

                        TaskSpec experimentTask = new TaskSpec(AnalyticsTask.TYPE, accession);
                        taskMan.updateTaskStage(experimentTask, TaskStatus.INCOMPLETE);
                        TaskSpec indexTask = new TaskSpec(IndexTask.TYPE, "");
                        taskMan.updateTaskStage(indexTask, TaskStatus.NONE);

                        if(!stop && isRunningAutoDependencies()) {
                            taskMan.scheduleTask(
                                    LoaderTask.this,
                                    experimentTask,
                                    TaskRunMode.RESTART,
                                    getUser(),
                                    true,
                                    "Automatically added by experiment " + getTaskSpec().getAccession() + " loading task");
                        }
                    } else if(TYPE_ARRAYDESIGN.equals(getTaskSpec().getType())) {
                        taskMan.addTaskTag(LoaderTask.this, TaskTagType.ARRAYDESIGN, accession);

                        TaskSpec indexTask = new TaskSpec(IndexTask.TYPE, "");
                        taskMan.updateTaskStage(indexTask, TaskStatus.INCOMPLETE);
                        if(!stop && isRunningAutoDependencies()) {
                            taskMan.scheduleTask(LoaderTask.this, indexTask, TaskRunMode.CONTINUE, getUser(), true,
                                    "Automatically added by array design " + getTaskSpec().getAccession() + " loading task");
                        }
                    }
                }
                taskMan.notifyTaskFinished(LoaderTask.this);
            }

            public void loadError(AtlasLoaderEvent event) {
                for(Throwable e : event.getErrors()) {
                    log.error("Task failed because of:", e);
                }
                taskMan.writeTaskLog(LoaderTask.this, TaskEvent.FAILED, StringUtils.join(event.getErrors(), '\n'));
                taskMan.notifyTaskFinished(LoaderTask.this);
            }

            public void loadProgress(String progress) {
                currentProgress = progress;
            }
        };

        taskMan.getLoader().setPossibleQTypes(taskMan.getAtlasProperties().getPossibleQuantitaionTypes());

        try {
            if(TYPE_EXPERIMENT.equals(getTaskSpec().getType()))
                taskMan.getLoader().loadExperiment(new URL(getTaskSpec().getAccession()), listener);
            else if(TYPE_ARRAYDESIGN.equals(getTaskSpec().getType()))
                taskMan.getLoader().loadArrayDesign(new URL(getTaskSpec().getAccession()), listener);
            else if(TYPE_UPDATEEXPERIMENT.equals(getTaskSpec().getType()))
                taskMan.getLoader().updateNetCDFForExperiment(getTaskSpec().getAccession(), listener);
            else {
                taskMan.writeTaskLog(LoaderTask.this, TaskEvent.FAILED, "Impossible happened");
                taskMan.notifyTaskFinished(LoaderTask.this);
            }
        } catch(MalformedURLException e) {
            taskMan.writeTaskLog(LoaderTask.this, TaskEvent.FAILED, "Invalid URL " + getTaskSpec().getAccession());
            taskMan.notifyTaskFinished(LoaderTask.this);
        } catch (Throwable e) {
            taskMan.writeTaskLog(LoaderTask.this, TaskEvent.FAILED, e.toString());
            taskMan.notifyTaskFinished(LoaderTask.this);
        }
    }

    public void stop() {
        stop = true;
    }

    public LoaderTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
        super(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        taskMan.addTaskTag(LoaderTask.this,
                TYPE_UPDATEEXPERIMENT.equals(taskSpec.getType()) ? TaskTagType.EXPERIMENT : TaskTagType.URL,
                getTaskSpec().getAccession());
    }

    public boolean isBlockedBy(Task by) {
        return false;
    }

    public static final TaskFactory FACTORY = new TaskFactory() {
        public QueuedTask createTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
            return new LoaderTask(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        }

        public boolean isFor(TaskSpec taskSpec) {
            return TYPE_EXPERIMENT.equals(taskSpec.getType())
                    || TYPE_ARRAYDESIGN.equals(taskSpec.getType())
                    || TYPE_UPDATEEXPERIMENT.equals(taskSpec.getType());
        }

    };
    
}

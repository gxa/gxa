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
import uk.ac.ebi.gxa.loader.*;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.net.MalformedURLException;
import java.util.Map;

/**
 * Set of loader tasks implementation. Handles loads of experiments and arry designs,
 * unloads of experiments and updates of experiment's NetCDFs.
 *
 * @author pashky
 */
public class LoaderTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(LoaderTask.class);

    public static final String TYPE_LOADEXPERIMENT = "loadexperiment";
    public static final String TYPE_LOADARRAYDESIGN = "loadarraydesign";
    public static final String TYPE_UPDATEEXPERIMENT = "updateexperiment";
    public static final String TYPE_UNLOADEXPERIMENT = "unloadexperiment";

    public static TaskSpec SPEC_UPDATEEXPERIMENT(String accession) {
        return new TaskSpec(TYPE_UPDATEEXPERIMENT, accession);
    }

    private volatile boolean stop = false;

    private final Map<String,String[]> userData;

    private AtlasLoaderCommand getLoaderCommand() throws MalformedURLException {
        if(TYPE_LOADEXPERIMENT.equals(getTaskSpec().getType()))
            return new LoadExperimentCommand(getTaskSpec().getAccession(),
                    taskMan.getAtlasProperties().getLoaderPossibleQuantitaionTypes(), userData);

        else if(TYPE_LOADARRAYDESIGN.equals(getTaskSpec().getType()))
            return new LoadArrayDesignCommand(getTaskSpec().getAccession(),
                    taskMan.getAtlasProperties().getLoaderGeneIdPriority());

        else if(TYPE_UPDATEEXPERIMENT.equals(getTaskSpec().getType()))
            return new UpdateNetCDFForExperimentCommand(getTaskSpec().getAccession());

        else if(TYPE_UNLOADEXPERIMENT.equals(getTaskSpec().getType()))
            return new UnloadExperimentCommand(getTaskSpec().getAccession());
        throw new IllegalStateException();
    }

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
                    if(TYPE_LOADEXPERIMENT.equals(getTaskSpec().getType()) ||
                            TYPE_UPDATEEXPERIMENT.equals(getTaskSpec().getType())) {
                        taskMan.addTaskTag(LoaderTask.this, TaskTagType.EXPERIMENT, accession);

                        if(TYPE_LOADEXPERIMENT.equals(getTaskSpec().getType()))
                            taskMan.updateTaskStage(SPEC_UPDATEEXPERIMENT(accession), TaskStatus.DONE);

                        TaskSpec analyticsTask = AnalyticsTask.SPEC_ANALYTICS(accession);
                        if(event.isRecomputeStatistics()) {
                            taskMan.updateTaskStage(analyticsTask, TaskStatus.INCOMPLETE);
                        } else {
                            log.info("Analytics was preserved in NetCDF, no need to recompute");
                        }

                        final TaskSpec indexTask = IndexTask.SPEC_INDEXEXPERIMENT(accession);
                        taskMan.updateTaskStage(indexTask, TaskStatus.INCOMPLETE);

                        if(!stop && isRunningAutoDependencies()) {
                            taskMan.scheduleTask(
                                    LoaderTask.this,
                                    event.isRecomputeStatistics() ? analyticsTask : indexTask,
                                    TaskRunMode.RESTART,
                                    getUser(),
                                    true,
                                    "Automatically added by experiment " + getTaskSpec().getAccession() + " loading task");
                        }
                    } else if(TYPE_UNLOADEXPERIMENT.equals(getTaskSpec().getType())) {
                        TaskSpec experimentTask = AnalyticsTask.SPEC_ANALYTICS(accession);
                        taskMan.updateTaskStage(experimentTask, TaskStatus.NONE);
                        TaskSpec indexTask = IndexTask.SPEC_INDEXEXPERIMENT(accession);
                        taskMan.updateTaskStage(indexTask, TaskStatus.NONE);

                        taskMan.updateTaskStage(IndexTask.SPEC_INDEXALL, TaskStatus.INCOMPLETE);
                        if(!stop && isRunningAutoDependencies()) {
                            taskMan.scheduleTask(LoaderTask.this, IndexTask.SPEC_INDEXALL, TaskRunMode.CONTINUE, getUser(), true,
                                    "Automatically added by experiment " + getTaskSpec().getAccession() + " unloading task");
                        }
                    } else if(TYPE_LOADARRAYDESIGN.equals(getTaskSpec().getType()) ) {
                        taskMan.addTaskTag(LoaderTask.this, TaskTagType.ARRAYDESIGN, accession);

                        for(Experiment experiment : taskMan.getAtlasDAO().getExperimentByArrayDesign(accession)) {
                            taskMan.addTaskTag(LoaderTask.this, TaskTagType.EXPERIMENT, experiment.getAccession());
                            taskMan.updateTaskStage(LoaderTask.SPEC_UPDATEEXPERIMENT(experiment.getAccession()), TaskStatus.INCOMPLETE);
                        }

                        if(!stop && isRunningAutoDependencies()) {
                            for(Experiment experiment : taskMan.getAtlasDAO().getExperimentByArrayDesign(accession)) {
                                taskMan.scheduleTask(LoaderTask.this, LoaderTask.SPEC_UPDATEEXPERIMENT(experiment.getAccession()),
                                        TaskRunMode.CONTINUE, getUser(), true,
                                        "Automatically added by array design " + getTaskSpec().getAccession() + " loading task");
                            }
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

            public void loadWarning(String message) {
                taskMan.writeTaskLog(LoaderTask.this, TaskEvent.WARNING, message);
            }
        };

        try {
            getLoaderCommand();
            taskMan.getLoader().doCommand(getLoaderCommand(), listener);
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

    public LoaderTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies, Map<String,String[]> userData) {
        super(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        this.userData = userData;
        taskMan.addTaskTag(LoaderTask.this,
                TYPE_UPDATEEXPERIMENT.equals(taskSpec.getType()) || TYPE_UNLOADEXPERIMENT.equals(taskSpec.getType())
                        ? TaskTagType.EXPERIMENT : TaskTagType.URL,
                getTaskSpec().getAccession());
    }

    public boolean isBlockedBy(Task by) {
        return false;
    }

    public static final TaskFactory FACTORY = new TaskFactory() {
        public QueuedTask createTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies, Map<String,String[]> userData) {
            return new LoaderTask(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies, userData);
        }

        public boolean isFor(TaskSpec taskSpec) {
            return TYPE_LOADEXPERIMENT.equals(taskSpec.getType())
                    || TYPE_LOADARRAYDESIGN.equals(taskSpec.getType())
                    || TYPE_UPDATEEXPERIMENT.equals(taskSpec.getType())
                    || TYPE_UNLOADEXPERIMENT.equals(taskSpec.getType());
        }

    };
    
}

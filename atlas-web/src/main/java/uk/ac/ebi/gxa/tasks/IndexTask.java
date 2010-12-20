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
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderCommand;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.Arrays;
import java.util.Map;

/**
 * Index builder tasks implementation. Can (re-)index just one experiment or entire index.
 * Statuses for those two types of tasks interact in a kind of a tricky was. If "index"
 * task status is set to INCOMPLETE, it practically means that all experiments are considered
 * as having invalid index. In turn, any INCOMPLETE indexexperiment means that the whole index could do
 * with rebuilding
 *
 * @author pashky
 */
public class IndexTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(IndexTask.class);
    
    public static final String TYPE_INDEX = "index";
    public static final String TYPE_INDEXEXPERIMENT = "indexexperiment";

    public static final TaskSpec SPEC_INDEXALL = new TaskSpec(TYPE_INDEX, "");

    public static TaskSpec SPEC_INDEXEXPERIMENT(String accession) {
        return new TaskSpec(TYPE_INDEXEXPERIMENT, accession);
    }

    private IndexBuilderCommand getIndexBuilderCommand() {
        if(TYPE_INDEXEXPERIMENT.equals(getTaskSpec().getType()))
            return new UpdateIndexForExperimentCommand(getTaskSpec().getAccession());
        return new IndexAllCommand();
    }

    public void start() {
        if(nothingToDo())
            return;

        startTimer();
        taskMan.updateTaskStage(getTaskSpec(), TaskStatus.INCOMPLETE);
        taskMan.writeTaskLog(IndexTask.this, TaskEvent.STARTED, "");
        try {
            taskMan.getIndexBuilder().doCommand(getIndexBuilderCommand(), new IndexBuilderListener() {
                public void buildSuccess() {
                    taskMan.writeTaskLog(IndexTask.this, TaskEvent.FINISHED, "");
                    taskMan.updateTaskStage(getTaskSpec(), TaskStatus.DONE);
                    if(TYPE_INDEX.equals(getTaskSpec().getType())) {
                        for(Experiment e : taskMan.getAtlasDAO().getAllExperiments())
                            taskMan.updateTaskStage(new TaskSpec(TYPE_INDEXEXPERIMENT, e.getAccession()), TaskStatus.DONE);
                    }
                    taskMan.notifyTaskFinished(IndexTask.this); // it's waiting for this
                }

                public void buildError(IndexBuilderEvent event) {
                    for (Throwable e : event.getErrors()) {
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
        if(TYPE_INDEXEXPERIMENT.equals(taskSpec.getType()))
            taskMan.addTaskTag(this,TaskTagType.EXPERIMENT, getTaskSpec().getAccession());
    }

    public boolean isBlockedBy(Task by) {
        if(TYPE_INDEXEXPERIMENT.equals(getTaskSpec().getType())) {
            return (Arrays.asList(
                    AnalyticsTask.TYPE,
                    LoaderTask.TYPE_UPDATEEXPERIMENT
            ).contains(by.getTaskSpec().getType()) && by.getTaskSpec().getAccession().equals(getTaskSpec().getAccession()))
                    || Arrays.asList(TYPE_INDEX, TYPE_INDEXEXPERIMENT).contains(by.getTaskSpec().getType());
        } else {
            return Arrays.asList(
                    TYPE_INDEX, TYPE_INDEXEXPERIMENT,
                    AnalyticsTask.TYPE,
                    LoaderTask.TYPE_LOADEXPERIMENT,
                    LoaderTask.TYPE_LOADARRAYDESIGN
            ).contains(by.getTaskSpec().getType());
        }
    }
    
    public static final TaskFactory FACTORY = new TaskFactory() {
        public QueuedTask createTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies, Map<String,String[]> userData) {
            return new IndexTask(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        }

        public boolean isFor(TaskSpec taskSpec) {
            return TYPE_INDEX.equals(taskSpec.getType()) || TYPE_INDEXEXPERIMENT.equals(taskSpec.getType());
        }

    };
}

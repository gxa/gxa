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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGenerationEvent;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Analytics task implementation. Re-builds anayltics, not much else.
 *
 * @author pashky
 */
public class AnalyticsTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(AnalyticsTask.class);

    public static final String TYPE = "analytics";

    public static TaskSpec SPEC_ANALYTICS(String accession) {
        return new TaskSpec(TYPE, accession);
    }

    private volatile boolean stop = false;

    public void start() {
        if (nothingToDo())
            return;

        startTimer();
        taskMan.updateTaskStage(getTaskSpec(), TaskStatus.INCOMPLETE);
        taskMan.writeTaskLog(AnalyticsTask.this, TaskEvent.STARTED, "");

        try {
            taskMan.getAnalyticsGenerator().generateAnalyticsForExperiment(getTaskSpec().getAccession(),
                    new AnalyticsGeneratorListener() {
                        public void buildSuccess() {
                            taskMan.writeTaskLog(AnalyticsTask.this, TaskEvent.FINISHED, "");
                            taskMan.updateTaskStage(getTaskSpec(), TaskStatus.DONE);

                            final TaskSpec indexTask = IndexTask.SPEC_INDEXEXPERIMENT(getTaskSpec().getAccession());
                            taskMan.updateTaskStage(indexTask, TaskStatus.INCOMPLETE);
                            if (!stop && isRunningAutoDependencies()) {
                                taskMan.scheduleTask(AnalyticsTask.this, indexTask, TaskRunMode.CONTINUE, getUser(), true,
                                        "Automatically added by experiment " + getTaskSpec().getAccession() + " processing task");
                            }
                            taskMan.notifyTaskFinished(AnalyticsTask.this);
                        }

                        public void buildError(AnalyticsGenerationEvent event) {
                            for (Throwable e : event.getErrors()) {
                                log.error("Task failed because of:", e);
                            }
                            taskMan.writeTaskLog(AnalyticsTask.this, TaskEvent.FAILED, StringUtils.join(Collections2.transform(event.getErrors(),
                                    new Function<Throwable, String>() {
                                        public String apply(@Nonnull Throwable e) {
                                            return e.getMessage() != null ? e.getMessage() : e.toString();
                                        }
                                    }), '\n'));
                            taskMan.notifyTaskFinished(AnalyticsTask.this);
                        }

                        public void buildProgress(String progressStatus) {
                            if (progressStatus.length() > 0)
                                log.info(progressStatus);
                            currentProgress = progressStatus;
                        }

                        public void buildWarning(String message) {
                            taskMan.writeTaskLog(AnalyticsTask.this, TaskEvent.WARNING, message);
                        }

                    });
        } catch (Throwable e) {
            taskMan.writeTaskLog(AnalyticsTask.this, TaskEvent.FAILED, e.toString());
            taskMan.notifyTaskFinished(AnalyticsTask.this);
        }
    }

    public AnalyticsTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
        super(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        taskMan.addTaskTag(AnalyticsTask.this, TaskTagType.EXPERIMENT, getTaskSpec().getAccession());
    }

    public void stop() {
        stop = true;
    }

    public boolean isBlockedBy(Task otherTask) {
        return Arrays.asList(
                LoaderTask.TYPE_LOADEXPERIMENT
        ).contains(otherTask.getTaskSpec().getType());
    }

    public static final TaskFactory FACTORY = new TaskFactory() {
        public QueuedTask createTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
            return new AnalyticsTask(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        }

        public boolean isFor(TaskSpec taskSpec) {
            return TYPE.equals(taskSpec.getType());
        }
    };
}

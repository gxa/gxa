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

import java.util.Map;

/**
 * "Repair experiment" task implementation. Does nothing by itself, only schedules appropriate tasks
 * according to various experiment data statuses - netcdf, analytics, index.
 * 
 * @author pashky
 */
public class RepairExperimentTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(RepairExperimentTask.class);

    public static final String TYPE = "repairexperiment";

    public void start() {
        final String accession = getTaskSpec().getAccession();
        log.info("Repair experiment - task started, checking NetCDF");

        final TaskSpec netcdfSpec = LoaderTask.SPEC_UPDATEEXPERIMENT(accession);
        final TaskStatus netcdfState = taskMan.getTaskStatus(netcdfSpec);
        if(TaskStatus.DONE != netcdfState) {
            taskMan.scheduleTask(this, netcdfSpec, TaskRunMode.CONTINUE, getUser(), true,
                    "Automatically added by experiment " + accession + " repair task");
            taskMan.notifyTaskFinished(this);
            return;
        }
        log.info("Repair experiment - NetCDF is complete, checking analytics");

        final TaskSpec analyticsSpec = AnalyticsTask.SPEC_ANALYTICS(accession);
        final TaskStatus analyticsState = taskMan.getTaskStatus(analyticsSpec);
        if(TaskStatus.DONE != analyticsState) {
            taskMan.scheduleTask(this, analyticsSpec, TaskRunMode.CONTINUE, getUser(), true,
                    "Automatically added by experiment " + accession + " repair task");
            taskMan.notifyTaskFinished(this);
            return;
        }
        log.info("Repair experiment - analytics is complete, checking index");

        final TaskSpec indexSpec = IndexTask.SPEC_INDEXEXPERIMENT(accession);
        final TaskStatus indexState = taskMan.getTaskStatus(indexSpec);
        if(TaskStatus.DONE != indexState) {
            taskMan.scheduleTask(this, indexSpec, TaskRunMode.CONTINUE, getUser(), true,
                    "Automatically added by experiment " + accession + " repair task");
            taskMan.notifyTaskFinished(this);
            return;
        }
        log.info("Repair experiment - index is complete, nothing to do");

        taskMan.notifyTaskFinished(this);
    }

    public void stop() {
    }

    public RepairExperimentTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
        super(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
    }

    public boolean isBlockedBy(Task otherTask) {
        return false;
    }

    public static final TaskFactory FACTORY = new TaskFactory() {
        public QueuedTask createTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies, Map<String,String[]> userData) {
            return new RepairExperimentTask(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        }

        public boolean isFor(TaskSpec taskSpec) {
            return TYPE.equals(taskSpec.getType());
        }
    };

}

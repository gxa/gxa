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

/**
 * @author pashky
 */
public abstract class AbstractWorkingTask implements WorkingTask {
    private final Task prototype;

    protected final TaskManager taskMan;
    protected volatile TaskStage currentStage;
    protected volatile String currentProgress;
    private long startTime;

    public AbstractWorkingTask(final TaskManager taskMan, final Task prototype) {
        this.taskMan = taskMan;
        this.prototype = prototype;
        this.currentStage = taskMan.getTaskStage(getTaskSpec());
        this.currentProgress = "";
        this.startTime = System.currentTimeMillis();
    }

    public TaskSpec getTaskSpec() {
        return prototype.getTaskSpec();
    }

    public TaskStage getCurrentStage() {
        return currentStage;
    }

    public TaskRunMode getRunMode() {
        return prototype.getRunMode();
    }

    public long getTaskId() {
        return prototype.getTaskId();
    }

    public TaskUser getUser() {
        return prototype.getUser();
    }

    public boolean isRunningAutoDependencies() {
        return prototype.isRunningAutoDependencies();
    }

    public String getCurrentProgress() {
        return currentProgress;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}

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
public abstract class AbstractWorkingTask implements WorkingTask, QueuedTask {
    private final long taskId;
    private final TaskSpec taskSpec;
    private TaskRunMode runMode;
    private final TaskUser user;
    private final boolean runningAutoDependencies;

    protected final TaskManager taskMan;
    protected volatile String currentProgress;
    private long startTime;

    protected AbstractWorkingTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
        this.taskId = taskId;
        this.taskSpec = taskSpec;
        this.runMode = runMode;
        this.user = user;
        this.runningAutoDependencies = runningAutoDependencies;
        this.taskMan = taskMan;
        this.currentProgress = "";
        this.startTime = System.currentTimeMillis();
    }

    public TaskSpec getTaskSpec() {
        return taskSpec;
    }

    public TaskRunMode getRunMode() {
        return runMode;
    }

    public long getTaskId() {
        return taskId;
    }

    public TaskUser getUser() {
        return user;
    }

    public boolean isRunningAutoDependencies() {
        return runningAutoDependencies;
    }

    public String getCurrentProgress() {
        return currentProgress;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    public void setRunMode(TaskRunMode runMode) {
        this.runMode = runMode; 
    }

    public WorkingTask getWorkingTask() {
        return this;
    }

    protected boolean nothingToDo() {
        if(getRunMode() == TaskRunMode.CONTINUE && TaskStatus.DONE.equals(taskMan.getTaskStatus(getTaskSpec()))) {
            taskMan.writeTaskLog(this, TaskEvent.SKIPPED, "");
            taskMan.notifyTaskFinished(this);
            return true;
        }
        return false;
    }

    protected void startTimer() {
        this.startTime = System.currentTimeMillis();
    }
}

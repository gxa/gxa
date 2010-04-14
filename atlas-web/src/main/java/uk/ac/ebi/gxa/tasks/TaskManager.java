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

import uk.ac.ebi.gxa.analytics.generator.AnalyticsGenerator;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.loader.AtlasLoader;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import java.util.*;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author pashky
 */
public class TaskManager implements InitializingBean {
    private static Logger log = LoggerFactory.getLogger(TaskManager.class);

    private AnalyticsGenerator analyticsGenerator;
    private IndexBuilder indexBuilder;
    private AtlasLoader<URL> loader;
    private AtlasProperties atlasProperties;

    private PersistentStorage storage;
    private volatile boolean running = true;
    private int maxWorkingTasks = 16;

    private static List<TaskFactory> taskFactories = new ArrayList<TaskFactory>();

    static {
        taskFactories.add(AnalyticsTask.FACTORY);
        taskFactories.add(IndexTask.FACTORY);
        taskFactories.add(LoaderTask.FACTORY);
        taskFactories.add(UnloadExperimentTask.FACTORY);
    }

    private final LinkedList<QueuedTask> queuedTasks = new LinkedList<QueuedTask>();

    private final LinkedHashSet<WorkingTask> workingTasks = new LinkedHashSet<WorkingTask>();

    public AtlasProperties getAtlasProperties() {
        return atlasProperties;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setStorage(PersistentStorage storage) {
        this.storage = storage;
    }

    AnalyticsGenerator getAnalyticsGenerator() {
        return analyticsGenerator;
    }

    public void setAnalyticsGenerator(AnalyticsGenerator analyticsGenerator) {
        this.analyticsGenerator = analyticsGenerator;
    }

    IndexBuilder getIndexBuilder() {
        return indexBuilder;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
    }

    public AtlasLoader<URL> getLoader() {
        return loader;
    }

    public void setLoader(AtlasLoader<URL> loader) {
        this.loader = loader;
    }

    public int getMaxWorkingTasks() {
        return maxWorkingTasks;
    }

    public void setMaxWorkingTasks(int maxWorkingTasks) {
        this.maxWorkingTasks = maxWorkingTasks;
    }

    public void afterPropertiesSet() throws Exception {
        start();
    }

    private long getNextId() {
        return storage.getNextTaskId();
    }

    private void insertTaskToQueue(QueuedTask task) {

        int insertTo = 0;
        int i = 0;
        for(QueuedTask queuedTask : queuedTasks) {
            if(task.isBlockedBy(queuedTask)) {
                insertTo = i + 1;
            }
            ++i;
        }
        queuedTasks.add(insertTo, task);
    }

    private QueuedTask getTaskInQueue(TaskSpec taskSpec) {
        for(QueuedTask queuedTask : queuedTasks) {
            if(queuedTask.getTaskSpec().equals(taskSpec)) {
                return queuedTask;
            }
        }
        return null;
    }

    public long scheduleTask(TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean autoAddDependent, String message) {
        return scheduleTask(null, taskSpec, runMode, user, autoAddDependent, message);
    }

    long scheduleTask(Task parentTask, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean autoAddDependent, String message) {
        synchronized(this) {
            log.info("Queuing task " + taskSpec + " in mode " + runMode + " as user " + user);

            QueuedTask alreadyThere = getTaskInQueue(taskSpec);
            if(alreadyThere != null) {
                log.info("Task is already queued, do not run it twice");
                if(alreadyThere.getRunMode() == TaskRunMode.CONTINUE && runMode == TaskRunMode.RESTART)
                    alreadyThere.setRunMode(runMode); // adjust run mode to more deep

                long alreadyThereId = alreadyThere.getTaskId();
                if(parentTask != null)
                    storage.joinTagCloud(parentTask, alreadyThere);
                return alreadyThereId;
            }

            // okay, we should run it propbably
            long taskId = getNextId();
            TaskFactory factory = getFactoryBySpec(taskSpec);
            QueuedTask proposedTask = factory.createTask(this, taskId, taskSpec, runMode, user, autoAddDependent);
            if(parentTask != null)
                storage.joinTagCloud(parentTask, proposedTask);
            storage.logTaskEvent(proposedTask, TaskEvent.SCHEDULED, message);

            insertTaskToQueue(proposedTask);

            if(running)
                runNextTask();

            return taskId;
        }
    }

    public List<WorkingTask> getWorkingTasks() {
        synchronized (this) {
            return new ArrayList<WorkingTask>(workingTasks);
        }
    }

    public  List<Task> getQueuedTasks() {
        synchronized (this) {
            return new ArrayList<Task>(queuedTasks);
        }
    }

    private Task getTaskById(final long taskId) {
        for(Task task : workingTasks)
            if(task.getTaskId() == taskId)
                return task;
        for(Task task : queuedTasks)
            if(task.getTaskId() == taskId)
                return task;
        return null;
    }

    private TaskFactory getFactoryBySpec(final TaskSpec taskSpec) {
        for(TaskFactory factory : taskFactories)
            if(factory.isFor(taskSpec))
                return factory;
        log.error("Can't find factory for task " + taskSpec);
        throw new IllegalStateException("Can't find factory for task " + taskSpec);
    }

    public void cancelAllTasks(TaskUser user, String message) {
        synchronized (this) {
            log.info("Cancelling all tasks");

            for(WorkingTask workingTask : workingTasks) {
                storage.logTaskEvent(workingTask, TaskEvent.CANCELLED, message);
                workingTask.stop();
            }
            for(QueuedTask queuedTask : queuedTasks) {
                storage.logTaskEvent(queuedTask, TaskEvent.CANCELLED, message);
            }
            queuedTasks.clear();
        }
    }

    public void cancelTask(long taskId, TaskUser user, String message) {
        synchronized (this) {
            log.info("Cancelling taskId " + taskId + " as user " + user);
            Task task = getTaskById(taskId);
            if(task == null) {
                log.info("Not found task id = " + taskId);
                return;
            }

            storage.logTaskEvent(task, TaskEvent.CANCELLED, message);
            for(WorkingTask workingTask : workingTasks)
                if(workingTask == task) { // identity check is intentional
                    log.info("It's working now, requesting to stop");
                    workingTask.stop();
                }

            for(QueuedTask queuedTask : queuedTasks) {
                if(queuedTask == task) { // identity check is intentional
                    queuedTasks.remove(queuedTask);
                    break;
                }
            }
        }
    }

    public void start() {
        log.info("Starting task manager");
        running = true;
        runNextTask();
    }

    public void pause() {
        log.info("Pausing task manager");
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isRunningSomething() {
        synchronized(this) {
            return !workingTasks.isEmpty() || !queuedTasks.isEmpty();
        }
    }

    private void runNextTask() {
        synchronized (this) {
            List<WorkingTask> toStart = new ArrayList<WorkingTask>();
            ListIterator<QueuedTask> queueIterator = queuedTasks.listIterator();
            while(queueIterator.hasNext()) {
                if(workingTasks.size() >= maxWorkingTasks)
                    return;
                
                QueuedTask nextTask = queueIterator.next();
                boolean blocked = false;
                for(WorkingTask workingTask : workingTasks) {
                    blocked |= nextTask.isBlockedBy(workingTask);
                    blocked |= workingTask.getTaskSpec().equals(nextTask.getTaskSpec()); // same spec is already working
                }
                if(!blocked) {
                    queueIterator.remove();
                    log.info("Task " + nextTask.getTaskSpec() + " is about to start in " + nextTask.getRunMode() + " mode");
                    WorkingTask workingTask = nextTask.getWorkingTask();
                    workingTasks.add(workingTask);
                    toStart.add(workingTask);
                }
            }
            for(WorkingTask ts : toStart)
                ts.start();
        }
    }

    void notifyTaskFinished(WorkingTask task) {
        synchronized (this) {
            log.info("Task " + task.getTaskSpec() + " finished");
            workingTasks.remove(task);
        }
        if(running)
            runNextTask();
    }

    void updateTaskStage(TaskSpec taskSpec, TaskStatus stage) {
        storage.updateTaskStatus(taskSpec, stage);
    }

    public TaskStatus getTaskStatus(TaskSpec taskSpec) {
        return storage.getTaskStatus(taskSpec);
    }

    void writeTaskLog(Task task, TaskEvent event, String message) {
        String logmsg = "Task " + task.getTaskSpec() + " " + event + " " + message;
        if(event == TaskEvent.FAILED)
            log.error(logmsg);
        else
            log.info(logmsg);
        
        storage.logTaskEvent(task, event, message);
    }

    void addTaskTag(Task task, TaskTagType type, String tag) {
        storage.addTag(task, type, tag);
    }
}

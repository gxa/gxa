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

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGenerator;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.loader.AtlasLoader;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import java.util.*;

/**
 * Task Manager for admin background tasks
 *
 * This class handles all loads, analytics and index rebuilds by managing a queue
 * of dependent tasks. The queue itself is not persistent and exists only in memory thus
 * only one atlas software instance should be running admin interface against particular database
 * to avoid collisions and errors
 *
 * @author pashky
 */
public class TaskManager  {
    private static Logger log = LoggerFactory.getLogger(TaskManager.class);

    private AnalyticsGenerator analyticsGenerator;
    private IndexBuilder indexBuilder;
    private AtlasLoader loader;
    private AtlasProperties atlasProperties;
    private AtlasDAO atlasDAO;

    private PersistentStorage storage;
    private volatile boolean running = true;
    /**
     * Default maximum number of simultaneously running tasks
     */
    private int maxWorkingTasks = 16;

    private static List<TaskFactory> taskFactories = new ArrayList<TaskFactory>();

    static {
        taskFactories.add(AnalyticsTask.FACTORY);
        taskFactories.add(IndexTask.FACTORY);
        taskFactories.add(LoaderTask.FACTORY);
        taskFactories.add(RepairExperimentTask.FACTORY);
    }

    private final List<QueuedTask> queuedTasks = new ArrayList<QueuedTask>();

    private final LinkedHashSet<WorkingTask> workingTasks = new LinkedHashSet<WorkingTask>();

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

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

    public AtlasLoader getLoader() {
        return loader;
    }

    public void setLoader(AtlasLoader loader) {
        this.loader = loader;
    }

    /**
     * Sets maximum number of simultaneously running tasks
     * @param maxWorkingTasks maximum number of simultaneously running tasks
     */
    public synchronized void setMaxWorkingTasks(int maxWorkingTasks) {
        this.maxWorkingTasks = maxWorkingTasks;
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

    /**
     * Schedules task to for execution
     * @param taskSpec task specification
     * @param runMode running mode - RESTART or CONTINUE
     * @param user responsible user
     * @param autoAddDependent true if dependent tasks should be scheduled automatically upon execution
     * @param message log comment string
     * @return task ID
     */
    public long scheduleTask(TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean autoAddDependent, String message) {
        return scheduleTask(taskSpec, runMode, user, autoAddDependent, message, Collections.<String,String[]>emptyMap());
    }

    public long scheduleTask(TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean autoAddDependent, String message, Map<String,String[]> userData) {
        return scheduleTask(null, taskSpec, runMode, user, autoAddDependent, message, userData);
    }

    long scheduleTask(Task parentTask, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean autoAddDependent, String message) {
        return scheduleTask(parentTask, taskSpec, runMode, user, autoAddDependent, message, Collections.<String,String[]>emptyMap());
    }

    long scheduleTask(Task parentTask, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean autoAddDependent, String message, Map<String,String[]> userData) {
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
            QueuedTask proposedTask = factory.createTask(this, taskId, taskSpec, runMode, user, autoAddDependent, userData);
            if(parentTask != null)
                storage.joinTagCloud(parentTask, proposedTask);
            storage.logTaskEvent(proposedTask, TaskEvent.SCHEDULED, message, null);

            insertTaskToQueue(proposedTask);

            if(running)
                runNextTask();

            return taskId;
        }
    }

    /**
     * Returns list of currently working tasks references (it's a snapshot, not a real one,
     * so don't expect any changes once it is returned). However, refernced tasks are real
     * so may change their state subsequently
     *
     * @return list of references to working tasks
     */
    public List<WorkingTask> getWorkingTasks() {
        synchronized (this) {
            return new ArrayList<WorkingTask>(workingTasks);
        }
    }

    /**
     * Returns list of currently working tasks references (it's a snapshot, not a real one,
     * so don't expect any changes once it is returned). However, refernced tasks are real
     * so may change their state subsequently
     *
     * @return list of references to queued tasks
     */
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

    /**
     * Cancel all existing tasks
     * @param user responsible user
     * @param message log comment
     */
    public void cancelAllTasks(TaskUser user, String message) {
        synchronized (this) {
            log.info("Cancelling all tasks");

            for(WorkingTask workingTask : workingTasks) {
                storage.logTaskEvent(workingTask, TaskEvent.CANCELLED, message, user);
                workingTask.stop();
            }
            for(QueuedTask queuedTask : queuedTasks) {
                storage.logTaskEvent(queuedTask, TaskEvent.CANCELLED, message, user);
            }
            queuedTasks.clear();
        }
    }

    /**
     * Cancel task by its ID
     * @param taskId task ID
     * @param user responsible user
     * @param message log comment
     */
    public void cancelTask(long taskId, TaskUser user, String message) {
        synchronized (this) {
            log.info("Cancelling taskId " + taskId + " as user " + user);
            Task task = getTaskById(taskId);
            if(task == null) {
                log.info("Not found task id = " + taskId);
                return;
            }

            storage.logTaskEvent(task, TaskEvent.CANCELLED, message, user);
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

    /**
     * Start queue execution (initially or after pause)
     */
    public void start() {
        log.info("Starting task manager");
        running = true;
        runNextTask();
    }

    /**
     * Pause task manager. All working tasks will finish but no more queued tasks will executed until
     * start() call
     */
    public void pause() {
        log.info("Pausing task manager");
        running = false;
    }

    /**
     * Check if task manager is running (not paused)
     * @return true or false
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Main "engine" method. Picks up the next available task from the queue tracking its dependencies.
     */
    private synchronized void runNextTask() {
        List<WorkingTask> toStart = new ArrayList<WorkingTask>();
        ListIterator<QueuedTask> queueIterator = queuedTasks.listIterator();
        while (queueIterator.hasNext()) {
            if (workingTasks.size() >= maxWorkingTasks)
                break;

            QueuedTask nextTask = queueIterator.next();
            boolean blocked = false;
            for (WorkingTask workingTask : workingTasks) {
                blocked |= nextTask.isBlockedBy(workingTask);
                blocked |= workingTask.getTaskSpec().equals(nextTask.getTaskSpec()); // same spec is already working
            }
            if (!blocked) {
                queueIterator.remove();
                log.info("Task " + nextTask.getTaskSpec() + " is about to start in " + nextTask.getRunMode() + " mode");
                WorkingTask workingTask = nextTask.getWorkingTask();
                workingTasks.add(workingTask);
                toStart.add(workingTask);
            }
        }
        for (WorkingTask ts : toStart)
            ts.start();
    }

    void notifyTaskFinished(WorkingTask task) {
        synchronized (this) {
            workingTasks.remove(task);
        }
        if(running)
            runNextTask();
    }

    void updateTaskStage(TaskSpec taskSpec, TaskStatus stage) {
        storage.updateTaskStatus(taskSpec, stage);
    }

    /**
     * Fetch current task status
     * @param taskSpec task sepcification
     * @return current status
     */
    public TaskStatus getTaskStatus(TaskSpec taskSpec) {
        return storage.getTaskStatus(taskSpec);
    }

    /**
     * Internal method allowing task implementation to log something
     * @param task task
     * @param event what happened
     * @param message log comment
     */
    void writeTaskLog(WorkingTask task, TaskEvent event, String message) {
        String logmsg = "Task " + task.getTaskSpec() + " " + event + " " + message;
        if(event == TaskEvent.FAILED)
            log.error(logmsg);
        else
            log.info(logmsg);

        if(event == TaskEvent.FINISHED && Strings.isNullOrEmpty(message)) {
            long elapsedTime = task.getElapsedTime() / 1000;
            message = String.format("Successfully finished in %d:%02d:%02d",
                    elapsedTime / 3600, (elapsedTime % 3600) / 60, elapsedTime % 60);
        }
        storage.logTaskEvent(task, event, message, null);
    }

    /**
     * Task log messages have a conception of "tags" and "task groups". Each scheduled task may trigger a whole
     * cascade of dependent task automatically scheduled upon original task completion. All those task executions
     * are considered related to each other and each of them may be realted to handling of some object like particular
     * experiment or array design accession or load URL. So, the whole group gets a bunch of tags like
     * "experiment=E-AFMX-5", "url=http://ae.uk/e-afmx-5.idf.txt" and "arraydesign=A-AFFY-123" so later one can search
     * for all the log messages somehow related (may be indirectly as arraydesign relates to experiment load, that's the
     * whole point) to particular object.
     *
     * @param task task
     * @param type tag type
     * @param tag tag acession
     */
    void addTaskTag(Task task, TaskTagType type, String tag) {
        storage.addTag(task, type, tag);
    }
}

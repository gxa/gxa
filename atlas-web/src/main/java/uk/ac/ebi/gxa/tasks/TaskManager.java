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
import java.util.concurrent.atomic.AtomicInteger;
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
    private AtomicInteger idGenerator = new AtomicInteger(0);
    private int maxWorkingTasks = 16;

    private static List<WorkingTaskFactory> taskFactories = new ArrayList<WorkingTaskFactory>();

    static {
        taskFactories.add(ExperimentTask.FACTORY);
        taskFactories.add(IndexTask.FACTORY);
        taskFactories.add(LoaderTask.FACTORY);
        taskFactories.add(UnloadExperimentTask.FACTORY);
    }

    private static class QueuedTask implements Task {
        private final int taskId;
        private final TaskSpec taskSpec;
        private TaskRunMode runMode;
        private final TaskStage stage;
        private final TaskUser user;
        private final boolean runningAutoDependencies;

        QueuedTask(int taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskStage stage,
                   TaskUser user, boolean runningAutoDependencies) {
            this.taskId = taskId;
            this.taskSpec = taskSpec;
            this.runMode = runMode;
            this.stage = stage;
            this.user = user;
            this.runningAutoDependencies = runningAutoDependencies;

        }

        public int getTaskId() {
            return taskId;
        }

        public TaskSpec getTaskSpec() {
            return taskSpec;
        }

        public TaskStage getCurrentStage() {
            return stage;
        }

        public TaskRunMode getRunMode() {
            return runMode;
        }

        public TaskUser getUser() {
            return user;
        }

        public boolean isRunningAutoDependencies() {
            return runningAutoDependencies;
        }

        public void setRunMode(TaskRunMode runMode) {
            this.runMode = runMode;
        }
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

    private int getNextId() {
        return idGenerator.incrementAndGet();
    }

    private void insertTaskToQueue(QueuedTask task) {

        int insertTo = 0;
        int i = 0;
        WorkingTaskFactory factory = getFactoryBySpec(task.getTaskSpec());
        for(QueuedTask queuedTask : queuedTasks) {
            if(factory.isBlockedBy(task.getTaskSpec(), queuedTask.getTaskSpec())) {
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

    public int enqueueTask(TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean autoAddDependent, String message) {
        synchronized(this) {
            log.info("Queuing task " + taskSpec + " in mode " + runMode + " as user " + user);
            storage.logTaskOperation(taskSpec, runMode, user, TaskOperation.ENQUEUE, message);

            QueuedTask alreadyThere = getTaskInQueue(taskSpec);
            if(alreadyThere != null) {
                log.info("Task is already queued, do not run it twice");
                if(alreadyThere.getRunMode() == TaskRunMode.CONTINUE && runMode == TaskRunMode.RESTART)
                    alreadyThere.setRunMode(runMode); // adjust run mode to more deep
                return alreadyThere.getTaskId();
            }

            // okay, we should run it propbably
            int taskId = getNextId();
            QueuedTask proposedTask = new QueuedTask(taskId, taskSpec, runMode, getTaskStage(taskSpec), user, autoAddDependent);

            insertTaskToQueue(proposedTask);

            if(running)
                runNextTask();

            return taskId;
        }
    }

    public Collection<WorkingTask> getWorkingTasks() {
        synchronized (this) {
            return new ArrayList<WorkingTask>(workingTasks);
        }
    }

    public  Collection<Task> getQueuedTasks() {
        synchronized (this) {
            return new ArrayList<Task>(queuedTasks);
        }
    }

    private Task getTaskById(final int taskId) {
        for(Task task : workingTasks)
            if(task.getTaskId() == taskId)
                return task;
        for(Task task : queuedTasks)
            if(task.getTaskId() == taskId)
                return task;
        return null;
    }

    private WorkingTaskFactory getFactoryBySpec(final TaskSpec taskSpec) {
        for(WorkingTaskFactory factory : taskFactories)
            if(factory.isForType(taskSpec))
                return factory;
        log.error("Can't find factory for task " + taskSpec);
        throw new IllegalStateException("Can't find factory for task " + taskSpec);
    }

    public void cancelAllTasks(TaskUser user, String message) {
        synchronized (this) {
            log.info("Cancelling all tasks");

            for(WorkingTask workingTask : workingTasks) {
                storage.logTaskOperation(workingTask.getTaskSpec(), null, user, TaskOperation.CANCEL, message);
                workingTask.stop();
            }
            for(QueuedTask queuedTask : queuedTasks) {
                storage.logTaskOperation(queuedTask.getTaskSpec(), null, user, TaskOperation.CANCEL, message);
            }
            queuedTasks.clear();
        }
    }

    public void cancelTask(int taskId, TaskUser user, String message) {
        synchronized (this) {
            log.info("Cancelling taskId " + taskId + " as user " + user);
            Task task = getTaskById(taskId);
            if(task == null) {
                log.info("Not found task id = " + taskId);
                return;
            }

            storage.logTaskOperation(task.getTaskSpec(), null, user, TaskOperation.CANCEL, message);
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
            ListIterator<QueuedTask> queueIterator = queuedTasks.listIterator();
            while(queueIterator.hasNext()) {
                if(workingTasks.size() >= maxWorkingTasks)
                    return;
                
                QueuedTask nextTask = queueIterator.next();
                WorkingTaskFactory nextFactory = getFactoryBySpec(nextTask.getTaskSpec());
                boolean blocked = false;
                for(WorkingTask workingTask : workingTasks) {
                    blocked |= nextFactory.isBlockedBy(nextTask.getTaskSpec(), workingTask.getTaskSpec());
                    blocked |= workingTask.getTaskSpec().equals(nextTask.getTaskSpec()); // same spec is already working
                }
                if(!blocked) {
                    queueIterator.remove();
                    log.info("Task " + nextTask.getTaskSpec() + " is about to start in " + nextTask.getRunMode() + " mode");
                    WorkingTask workingTask = nextFactory.createTask(this, nextTask);
                    workingTasks.add(workingTask);
                    workingTask.start();
                }
            }
        }
    }

    void notifyTaskFinished(WorkingTask task) {
        synchronized (this) {
            log.info("Task " + task.getTaskSpec() + " finished at stage " + task.getCurrentStage());
            workingTasks.remove(task);
        }
        if(running)
            runNextTask();
    }

    void updateTaskStage(TaskSpec taskSpec, TaskStage stage) {
        storage.updateTaskStage(taskSpec, stage);
    }

    public TaskStage getTaskStage(TaskSpec taskSpec) {
        return storage.getTaskStage(taskSpec);
    }

    void writeTaskLog(TaskSpec taskSpec, TaskStage stage, TaskStageEvent event, String message) {
        String logmsg = "Task " + taskSpec + " " + event + " at stage " + stage + " " + message;
        if(event == TaskStageEvent.FAILED)
            log.error(logmsg);
        else
            log.info(logmsg);
        storage.logTaskStageEvent(taskSpec, stage, event, message);
    }

}

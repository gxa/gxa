package uk.ac.ebi.gxa.tasks;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGenerator;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGenerationEvent;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGenerator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;
import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGeneratorListener;
import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGenerationEvent;

import java.util.concurrent.TimeUnit;
import java.util.*;

import static junit.framework.Assert.*;

/**
 * @author pashky
 */
public class TaskManagerTest {
    static Logger log = LoggerFactory.getLogger(TaskManagerTest.class);

    TaskManager manager;
    TestStorage storage;

    static final List<Throwable> ERRORS = Collections.<Throwable>singletonList(new Exception("Your biggest mistake"));
    static final TaskUser defaultUser = new TaskUser("DaFatControlla");
    private static final int DONOTHINGNUM = 5;

    static void delay() {
        try { Thread.sleep(100); } catch(Exception e) {/**/}
    }

    void waitForManager() {
        log.info("Waiting for task manager...");
        int wait = 0;
        while(manager.isRunningSomething()) {
            delay();
            if(++wait > 100)
                fail("Timeout");
        }
        log.info("Task manager finished");
    }
    static class TestStorage implements PersistentStorage {
        static class TaskLogItem {
            public TaskSpec task;
            public TaskStage stage;
            public TaskStageEvent event;
            public String message;

            TaskLogItem(TaskSpec task, TaskStage stage, TaskStageEvent event, String message) {
                this.task = task;
                this.stage = stage;
                this.event = event;
                this.message = message;
            }
        }

        List<TaskLogItem> taskLog = new ArrayList<TaskLogItem>();

        static class OperLogItem {
            public TaskSpec task;
            public TaskRunMode runMode;
            public TaskUser user;
            public TaskOperation operation;
            public String message;

            OperLogItem(TaskSpec task, TaskRunMode runMode, TaskUser user, TaskOperation operation, String message) {
                this.task = task;
                this.runMode = runMode;
                this.user = user;
                this.operation = operation;
                this.message = message;
            }
        }

        List<OperLogItem> operLog = new ArrayList<OperLogItem>();

        Map<TaskSpec, TaskStage> taskStages = new HashMap<TaskSpec, TaskStage>();

        public synchronized void logTaskStageEvent(TaskSpec task, TaskStage stage, TaskStageEvent event, String message) {
            taskLog.add(new TaskLogItem(task, stage, event, message));
            log.info("Task Log " + task + " " + stage + " " + event + " " + message);
        }

        public synchronized void updateTaskStage(TaskSpec task, TaskStage stage) {
            taskStages.put(task, stage);
            log.info("Stage Log " + task + " " + stage);
        }

        public synchronized TaskStage getTaskStage(TaskSpec task) {
            return taskStages.get(task) != null ? taskStages.get(task) : TaskStage.NONE;
        }

        public synchronized void logTaskOperation(TaskSpec task, TaskRunMode runMode, TaskUser user, TaskOperation operation, String message) {
            operLog.add(new OperLogItem(task, runMode, user, operation, message));
            log.info("Operation Log " + task + " " + runMode + " " + user + " " + operation + " " + message);
        }
    }

    @Before
    public void setup() {
        manager = new TaskManager();

        manager.setAnalyticsGenerator(new AnalyticsGenerator() {
            boolean fixEverything = false;

            public void startup() throws AnalyticsGeneratorException {
                fixEverything = true;
            }

            public void shutdown() throws AnalyticsGeneratorException {
            }

            public void generateAnalytics() {
            }

            public void generateAnalytics(AnalyticsGeneratorListener listener) {
            }

            public void generateAnalyticsForExperiment(String experimentAccession) {
            }

            public void generateAnalyticsForExperiment(final String experimentAccession, final AnalyticsGeneratorListener listener) {
                // this one we would use
                log.info("Starting to generate analytics for " + experimentAccession);
                new Thread() {
                    @Override
                    public void run() {
                        for(int i = 0; i < DONOTHINGNUM; ++i) {
                            log.info("Heavy analytics calculations " + i + " for " + experimentAccession);
                            listener.buildProgress("Heavy analytics calculations " + i + " for " + experimentAccession);
                            delay();
                        }
                        if(fixEverything || experimentAccession.contains("A"))
                            listener.buildSuccess(new AnalyticsGenerationEvent(1000, TimeUnit.MILLISECONDS));
                        else
                            listener.buildError(new AnalyticsGenerationEvent(1000, TimeUnit.MILLISECONDS, ERRORS));
                    }
                }.start();
            }
        });

        manager.setNetcdfGenerator(new NetCDFGenerator() {
            boolean fixEverything = false;

            public void startup() throws NetCDFGeneratorException {
                fixEverything = true;
            }

            public void shutdown() throws NetCDFGeneratorException {
            }

            public void generateNetCDFs() {
            }

            public void generateNetCDFs(NetCDFGeneratorListener listener) {
            }

            public void generateNetCDFsForExperiment(String experimentAccession) {
            }

            public void generateNetCDFsForExperiment(final String experimentAccession, final NetCDFGeneratorListener listener) {
                // this one we would use
                log.info("Starting to generate netcdfs for " + experimentAccession);
                new Thread() {
                    @Override
                    public void run() {
                        for(int i = 0; i < DONOTHINGNUM; ++i) {
                            log.info("Flooding your disk with netcdfs for " + i + " for " + experimentAccession);
                            listener.buildProgress("Flooding your disk with netcdfs for " + i + " for " + experimentAccession);
                            delay();
                        }
                        if(fixEverything || experimentAccession.contains("N"))
                            listener.buildSuccess(new NetCDFGenerationEvent(1000, TimeUnit.MILLISECONDS));
                        else
                            listener.buildError(new NetCDFGenerationEvent(1000, TimeUnit.MILLISECONDS, ERRORS));
                    }
                }.start();
            }
        });

        manager.setIndexBuilder(new IndexBuilder() {
            boolean shouldFail = false;
            public void setIncludeIndexes(List<String> includeIndexes) {
            }

            public List<String> getIncludeIndexes() {
                return null;
            }

            public void startup() throws IndexBuilderException {
            }

            public void shutdown() throws IndexBuilderException {
                shouldFail = true; // a hack
            }

            public void buildIndex() {
            }

            public void buildIndex(final IndexBuilderListener listener) {
                log.info("Building indexes");
                new Thread() {
                    @Override
                    public void run() {
                        for(int i = 0; i < DONOTHINGNUM; ++i) {
                            log.info("Index building " + i);
                            listener.buildProgress("Index building " + i);
                            delay();
                        }
                        if(shouldFail)
                            listener.buildError(new IndexBuilderEvent(1000, TimeUnit.MILLISECONDS, ERRORS));
                        else
                            listener.buildSuccess(new IndexBuilderEvent(1000, TimeUnit.MILLISECONDS));
                    }
                }.start();
            }

            public void updateIndex() {
            }

            public void updateIndex(final IndexBuilderListener listener) {
                buildIndex(listener);
            }

            public void registerIndexBuildEventHandler(IndexBuilderEventHandler handler) {
            }

            public void unregisterIndexBuildEventHandler(IndexBuilderEventHandler handler) {
            }
        });

        manager.setStorage(storage = new TestStorage());

    }

    /**
     * Checks correct workflow and logs for just one simplest (index) task
     */
    @Test
    public void test_basicWorkflowForIndexTask() {
        TaskSpec spec = new TaskSpec("index", "");

        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, true);
        waitForManager();

        assertEquals(1, storage.taskStages.size());
        assertTrue(storage.taskStages.containsKey(spec));
        assertEquals(TaskStage.DONE, storage.taskStages.get(spec));

        assertEquals(2, storage.taskLog.size());
        assertEquals(spec, storage.taskLog.get(0).task);
        assertEquals("INDEX", storage.taskLog.get(0).stage.toString());
        assertEquals(TaskStageEvent.STARTED, storage.taskLog.get(0).event);

        assertEquals(spec, storage.taskLog.get(1).task);
        assertEquals("INDEX", storage.taskLog.get(1).stage.toString());
        assertEquals(TaskStageEvent.FINISHED, storage.taskLog.get(1).event);

        assertEquals(1, storage.operLog.size());
        assertEquals(spec, storage.operLog.get(0).task);
        assertEquals(defaultUser, storage.operLog.get(0).user);
        assertEquals(TaskRunMode.RESTART, storage.operLog.get(0).runMode);
        assertEquals(TaskOperation.ENQUEUE, storage.operLog.get(0).operation);
    }

    /**
     * Checks if successful experiment build marks index as dirty
     */
    @Test
    public void test_markIndexAsDirtyAfterSuccessfulExperiment() {
        TaskSpec spec = new TaskSpec("experiment", "E-AN-1");
        TaskSpec speci = new TaskSpec("index", "");

        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, false);
        waitForManager();

        assertEquals(TaskStage.DONE, storage.taskStages.get(spec));
        assertEquals(IndexTask.INDEX_STAGE, storage.taskStages.get(speci));
    }

    /**
     * Checks if failed experiment build DOES NOT mark index as dirty
     */
    @Test
    public void test_markIndexAsDirtySometimesNot() {
        TaskSpec spec = new TaskSpec("experiment", "E-A-1");
        TaskSpec speci = new TaskSpec("index", "");

        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, false);
        waitForManager();

        assertNotSame(TaskStage.DONE, storage.taskStages.get(spec)); // failed experiment
        assertNull(storage.taskStages.get(speci)); // no change to index status
    }

    /**
     * Checks correct workflow and logs for task which has auto-added dependencies
     */
    @Test
    public void test_dependencyWorkflowForIndexTask() {
        TaskSpec spec = new TaskSpec("experiment", "E-AN-1");

        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, true);
        waitForManager();

        assertEquals(2, storage.taskStages.size());
        assertTrue(storage.taskStages.containsKey(spec));
        assertEquals(TaskStage.DONE, storage.taskStages.get(spec));
        TaskSpec speci = new TaskSpec("index", "");
        assertTrue(storage.taskStages.containsKey(speci));
        assertEquals(TaskStage.DONE, storage.taskStages.get(speci));


        assertEquals(6, storage.taskLog.size());
        assertEquals(spec, storage.taskLog.get(0).task);
        assertEquals("NETCDF", storage.taskLog.get(0).stage.toString());
        assertEquals(TaskStageEvent.STARTED, storage.taskLog.get(0).event);

        assertEquals(spec, storage.taskLog.get(1).task);
        assertEquals("NETCDF", storage.taskLog.get(1).stage.toString());
        assertEquals(TaskStageEvent.FINISHED, storage.taskLog.get(1).event);

        assertEquals(spec, storage.taskLog.get(2).task);
        assertEquals("ANALYTICS", storage.taskLog.get(2).stage.toString());
        assertEquals(TaskStageEvent.STARTED, storage.taskLog.get(2).event);

        assertEquals(spec, storage.taskLog.get(3).task);
        assertEquals("ANALYTICS", storage.taskLog.get(3).stage.toString());
        assertEquals(TaskStageEvent.FINISHED, storage.taskLog.get(3).event);

        assertEquals(speci, storage.taskLog.get(4).task);
        assertEquals("INDEX", storage.taskLog.get(4).stage.toString());
        assertEquals(TaskStageEvent.STARTED, storage.taskLog.get(4).event);

        assertEquals(speci, storage.taskLog.get(5).task);
        assertEquals("INDEX", storage.taskLog.get(5).stage.toString());
        assertEquals(TaskStageEvent.FINISHED, storage.taskLog.get(5).event);

        assertEquals(2, storage.operLog.size());
        assertEquals(spec, storage.operLog.get(0).task);
        assertEquals(defaultUser, storage.operLog.get(0).user);
        assertEquals(TaskRunMode.RESTART, storage.operLog.get(0).runMode);
        assertEquals(TaskOperation.ENQUEUE, storage.operLog.get(0).operation);
        assertEquals(speci, storage.operLog.get(1).task);
        assertEquals(defaultUser, storage.operLog.get(1).user);
        assertEquals(TaskRunMode.RESTART, storage.operLog.get(1).runMode);
        assertEquals(TaskOperation.ENQUEUE, storage.operLog.get(1).operation);
    }

    /**
     * Checks if request to continue/restart from successfully completed task works correctly
     */
    @Test
    public void test_continueAndRestartFromSuccess() {
        TaskSpec spec = new TaskSpec("experiment", "EXP-AN-1");
        TaskSpec speci = new TaskSpec("index", "");

        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, true);
        waitForManager();
        assertEquals(2, storage.operLog.size()); // 1 for experiment, 1 for auto-index
        assertEquals(6, storage.taskLog.size()); // 4 for experiment, 2 for index
        assertEquals(TaskStage.DONE, storage.taskStages.get(spec));
        assertEquals(TaskStage.DONE, storage.taskStages.get(speci));

        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, true);
        waitForManager();
        assertEquals(4, storage.operLog.size()); // check we've logged the request, +1 for exp, +1 for auto-index
        assertEquals(6, storage.taskLog.size()); // ...but done nothing, as it's already done (including auto-added index task!)
        assertEquals(TaskStage.DONE, storage.taskStages.get(spec));
        assertEquals(TaskStage.DONE, storage.taskStages.get(speci));

        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, true);
        waitForManager();
        assertEquals(6, storage.operLog.size()); // check we've logged the request, +1 for exp, +1 for auto-index
        assertEquals(12, storage.taskLog.size()); // ...and did it once again
        assertEquals(TaskStage.DONE, storage.taskStages.get(spec));
        assertEquals(TaskStage.DONE, storage.taskStages.get(speci));
    }

    /**
     * ...and same for experiment, failing at one stage
     */
    @Test
    public void test_continueAndRestartFromFail() throws Exception {
        TaskSpec spec = new TaskSpec("experiment", "EXP-N-1");
        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, false); // those dependcies are in the way, so don't use them
        waitForManager();
        assertEquals(1, storage.operLog.size());
        assertEquals(4, storage.taskLog.size());
        assertEquals(TaskStage.valueOf("ANALYTICS"), storage.taskStages.get(spec));

        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, false);
        waitForManager();
        assertEquals(2, storage.operLog.size());
        assertEquals(6, storage.taskLog.size()); // tried only analytics one more time
        assertEquals(TaskStage.valueOf("ANALYTICS"), storage.taskStages.get(spec)); // ...and failed once again

        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, false);
        waitForManager();
        assertEquals(3, storage.operLog.size());
        assertEquals(10, storage.taskLog.size()); // tried both stages one more time, thus +4 log entries
        assertEquals(TaskStage.valueOf("ANALYTICS"), storage.taskStages.get(spec)); // ...and it's still there

        // MAGIC!!! it will cure failing analysis (see mock generator code)
        manager.getAnalyticsGenerator().startup();

        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, false);
        waitForManager();
        assertEquals(4, storage.operLog.size());
        assertEquals(12, storage.taskLog.size()); // one more attempt
        assertEquals(TaskStage.DONE, storage.taskStages.get(spec)); // and it should be fine now

        // ...and once more from the very start to be sure it really works
        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, false);
        waitForManager();
        assertEquals(5, storage.operLog.size());
        assertEquals(16, storage.taskLog.size()); // one more attempt
        assertEquals(TaskStage.DONE, storage.taskStages.get(spec)); // and it still should be fine 
    }

    @Test
    public void test_cancelWorkingTask() throws Exception {
        TaskSpec spec = new TaskSpec("experiment", "EXP-AN-1");
        int id = manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, false); // no index task again
        delay(); // let it do something
        manager.cancelTask(id, defaultUser); // and now kill it
        waitForManager();
        assertEquals(2, storage.operLog.size()); // 2 ops: queue and cancel
        assertEquals(3, storage.taskLog.size()); // one stage is done, the other is stopped
        assertEquals(TaskStage.valueOf("ANALYTICS"), storage.taskStages.get(spec)); // ...and no analytics is here

        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, false);
        waitForManager();
        assertEquals(3, storage.operLog.size()); // one more start
        assertEquals(5, storage.taskLog.size()); // now it should do the missing stage successfully
        assertEquals(TaskStage.DONE, storage.taskStages.get(spec)); // and it should be fine now
    }

    @Test
    public void test_cancelPendingTask() throws Exception {
        manager.pause();
        TaskSpec spece1 = new TaskSpec("experiment", "EXP-AN-1");
        manager.enqueueTask(spece1, TaskRunMode.CONTINUE, defaultUser, true);

        // find auto-added index task
        TaskSpec specidx = new TaskSpec("index", "");
        int taskIdxId = -1;
        for(Task task : manager.getQueuedTasks())
            if(task.getTaskSpec().equals(specidx))
                taskIdxId = task.getTaskId();

        manager.start();
        delay(); // let it do something
        manager.cancelTask(taskIdxId, defaultUser); // change our mind, cancel auto-added task
        waitForManager();
        assertEquals(3, storage.operLog.size()); // 2 q's and 1 cancel
        assertEquals(4, storage.taskLog.size()); // first task is completed succesfully, but no trace of second task
        assertEquals(TaskStage.DONE, storage.taskStages.get(spece1)); // ...and no analytics is here
        assertNotSame(TaskStage.DONE, storage.taskStages.get(specidx)); // indexing was not run!
    }

    @Test
    public void test_dependenciesSequence() {
        TaskSpec spece1 = new TaskSpec("experiment", "E-AN-1");
        TaskSpec spece2 = new TaskSpec("experiment", "E-AN-2");
        TaskSpec speci = new TaskSpec("index", "");

        manager.pause();
        manager.enqueueTask(spece1, TaskRunMode.CONTINUE, defaultUser, true);
        manager.enqueueTask(spece2, TaskRunMode.CONTINUE, defaultUser, true);
        manager.enqueueTask(speci, TaskRunMode.CONTINUE, defaultUser, true);

        List<Task> tasks = new ArrayList<Task>(manager.getQueuedTasks());
        assertEquals(3, tasks.size()); // 3 tasks should be here        
        assertEquals(spece1, tasks.get(1).getTaskSpec());
        assertEquals(spece2, tasks.get(0).getTaskSpec());
        assertEquals(speci, tasks.get(2).getTaskSpec());

        manager.start();
        waitForManager();
        // TODO: check logs for correct running order - experiments 1 & 2 in parallel and index after
    }


}

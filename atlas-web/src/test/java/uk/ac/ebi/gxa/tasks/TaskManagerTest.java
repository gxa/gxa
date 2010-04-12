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
import uk.ac.ebi.gxa.loader.AtlasLoader;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.AtlasUnloaderException;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderEvent;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.Storage;

import java.util.concurrent.TimeUnit;
import java.util.*;
import java.net.URL;

import static junit.framework.Assert.*;

/**
 * @author pashky
 */
public class TaskManagerTest {
//    static Logger log = LoggerFactory.getLogger(TaskManagerTest.class);
//
//    TaskManager manager;
//    TestStorage storage;
//
//    static final List<Throwable> ERRORS = Collections.<Throwable>singletonList(new Exception("Your biggest mistake"));
//    static final TaskUser defaultUser = new TaskUser("DaFatControlla");
//    private static final int DONOTHINGNUM = 5;
//
//    static void delay() {
//        try { Thread.sleep(100); } catch(Exception e) {/**/}
//    }
//
//    void waitForManager() {
//        log.info("Waiting for task manager...");
//        int wait = 0;
//        while(manager.isRunningSomething()) {
//            delay();
//            if(++wait > 100)
//                fail("Timeout");
//        }
//        log.info("Task manager finished");
//    }
//    static class TestStorage implements PersistentStorage {
//        static class TaskLogItem {
//            public TaskSpec task;
//            public TaskStatus stage;
//            public TaskEvent event;
//            public String message;
//
//            TaskLogItem(TaskSpec task, TaskStatus stage, TaskEvent event, String message) {
//                this.task = task;
//                this.stage = stage;
//                this.event = event;
//                this.message = message;
//            }
//        }
//
//        List<TaskLogItem> taskLog = new ArrayList<TaskLogItem>();
//
//        static class OperLogItem {
//            public TaskSpec task;
//            public TaskRunMode runMode;
//            public TaskUser user;
//            public TaskOperation operation;
//            public String message;
//
//            OperLogItem(TaskSpec task, TaskRunMode runMode, TaskUser user, TaskOperation operation, String message) {
//                this.task = task;
//                this.runMode = runMode;
//                this.user = user;
//                this.operation = operation;
//                this.message = message;
//            }
//        }
//
//        List<OperLogItem> operLog = new ArrayList<OperLogItem>();
//
//        Map<TaskSpec, TaskStatus> taskStages = new HashMap<TaskSpec, TaskStatus>();
//
//        public synchronized void logTaskStageEvent(TaskSpec task, TaskEvent event, String message) {
//            taskLog.add(new TaskLogItem(task, stage, event, message));
//            log.info("Task Log " + task + " " + stage + " " + event + " " + message);
//        }
//
//        public synchronized void updateTaskStage(TaskSpec task, TaskStatus stage) {
//            taskStages.put(task, stage);
//            log.info("Stage Log " + task + " " + stage);
//        }
//
//        public synchronized TaskStatus getTaskStage(TaskSpec task) {
//            return taskStages.get(task) != null ? taskStages.get(task) : TaskStatus.NONE;
//        }
//
//        public synchronized void logTaskEvent(TaskSpec task, TaskRunMode runMode, TaskUser user, TaskOperation operation, String message) {
//            operLog.add(new OperLogItem(task, runMode, user, operation, message));
//            log.info("Operation Log " + task + " " + runMode + " " + user + " " + operation + " " + message);
//        }
//    }
//
//    @Before
//    public void setup() {
//        manager = new TaskManager();
//
//        manager.setAnalyticsGenerator(new AnalyticsGenerator() {
//            boolean fixEverything = false;
//
//            public void startup() throws AnalyticsGeneratorException {
//                fixEverything = true;
//            }
//
//            public void shutdown() throws AnalyticsGeneratorException {
//            }
//
//            public void generateAnalytics() {
//            }
//
//            public void generateAnalytics(AnalyticsGeneratorListener listener) {
//            }
//
//            public void generateAnalyticsForExperiment(String experimentAccession) {
//            }
//
//            public void generateAnalyticsForExperiment(final String experimentAccession, final AnalyticsGeneratorListener listener) {
//                // this one we would use
//                log.info("Starting to generate analytics for " + experimentAccession);
//                new Thread() {
//                    @Override
//                    public void run() {
//                        for(int i = 0; i < DONOTHINGNUM; ++i) {
//                            log.info("Heavy analytics calculations " + i + " for " + experimentAccession);
//                            listener.buildProgress("Heavy analytics calculations " + i + " for " + experimentAccession);
//                            delay();
//                        }
//                        if(fixEverything || experimentAccession.contains("A"))
//                            listener.buildSuccess(new AnalyticsGenerationEvent(1000, TimeUnit.MILLISECONDS));
//                        else
//                            listener.buildError(new AnalyticsGenerationEvent(1000, TimeUnit.MILLISECONDS, ERRORS));
//                    }
//                }.start();
//            }
//        });
//
//        manager.setIndexBuilder(new IndexBuilder() {
//            boolean shouldFail = false;
//            public void setIncludeIndexes(List<String> includeIndexes) {
//            }
//
//            public List<String> getIncludeIndexes() {
//                return null;
//            }
//
//            public void startup() throws IndexBuilderException {
//            }
//
//            public void shutdown() throws IndexBuilderException {
//                shouldFail = true; // a hack
//            }
//
//            public void buildIndex() {
//            }
//
//            public void buildIndex(final IndexBuilderListener listener) {
//                log.info("Building indexes");
//                new Thread() {
//                    @Override
//                    public void run() {
//                        for(int i = 0; i < DONOTHINGNUM; ++i) {
//                            log.info("Index building " + i);
//                            listener.buildProgress("Index building " + i);
//                            delay();
//                        }
//                        if(shouldFail)
//                            listener.buildError(new IndexBuilderEvent(1000, TimeUnit.MILLISECONDS, ERRORS));
//                        else
//                            listener.buildSuccess(new IndexBuilderEvent(1000, TimeUnit.MILLISECONDS));
//                    }
//                }.start();
//            }
//
//            public void updateIndex() {
//            }
//
//            public void updateIndex(final IndexBuilderListener listener) {
//                buildIndex(listener);
//            }
//
//            public void registerIndexBuildEventHandler(IndexBuilderEventHandler handler) {
//            }
//
//            public void unregisterIndexBuildEventHandler(IndexBuilderEventHandler handler) {
//            }
//        });
//
//        manager.setLoader(new AtlasLoader<URL>() {
//            boolean shouldFail = false;
//            public void setMissingDesignElementsCutoff(double missingDesignElementsCutoff) { }
//            public double getMissingDesignElementsCutoff() { return 0; }
//            public void setAllowReloading(boolean allowReloading) { }
//            public boolean getAllowReloading() { return false; }
//            public List<String> getGeneIdentifierPriority() { return null; }
//            public void setGeneIdentifierPriority(List<String> geneIdentifierPriority) { }
//
//            public void setPossibleQTypes(Collection<String> possibleQTypes) {
//
//            }
//
//            public Set<String> getPossibleQTypes() {
//                return null;
//            }
//
//            public void startup() throws AtlasLoaderException {
//                shouldFail = false; // a hack
//            }
//            public void shutdown() throws AtlasLoaderException {
//                shouldFail = true; // a hack
//            }
//
//            public void loadExperiment(URL experimentResource) { }
//            public void loadArrayDesign(URL arrayDesignResource) { }
//
//            public void loadExperiment(final URL url, final AtlasLoaderListener listener) {
//                log.info("Loading experiment " + url);
//                new Thread() {
//                    @Override
//                    public void run() {
//                        for(int i = 0; i < DONOTHINGNUM; ++i) {
//                            log.info("Loading experiment " + url + " " + i);
//                            listener.loadProgress("Parsing " + (i*100/DONOTHINGNUM) + "%");
//                            delay();
//                        }
//                        if(shouldFail)
//                            listener.loadError(AtlasLoaderEvent.error(1000, TimeUnit.MILLISECONDS, ERRORS));
//                        else
//                            listener.loadSuccess(AtlasLoaderEvent.success(1000, TimeUnit.MILLISECONDS,
//                                    Collections.singletonList(url.getPath().substring(1))));
//                    }
//                }.start();
//            }
//
//            public void loadArrayDesign(final URL url, final AtlasLoaderListener listener) {
//                log.info("Loading array design " + url);
//                new Thread() {
//                    @Override
//                    public void run() {
//                        for(int i = 0; i < DONOTHINGNUM; ++i) {
//                            log.info("Loading array design " + url + " " + i);
//                            listener.loadProgress("Parsing " + (i*100/DONOTHINGNUM) + "%");
//                            delay();
//                        }
//                        if(shouldFail)
//                            listener.loadError(AtlasLoaderEvent.error(1000, TimeUnit.MILLISECONDS, ERRORS));
//                        else
//                            listener.loadSuccess(AtlasLoaderEvent.success(1000, TimeUnit.MILLISECONDS,
//                                    Collections.singletonList(url.getPath().substring(1))));
//                    }
//                }.start();
//            }
//
//            public void unloadExperiment(String accession) throws AtlasUnloaderException {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//
//            public void unloadArrayDesign(String accession) throws AtlasUnloaderException {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//        });
//
//        manager.setStorage(storage = new TestStorage());
//
//        AtlasProperties props = new AtlasProperties();
//        props.setStorage(new Storage() {
//            public void setProperty(String name, String value) {
//
//            }
//
//            public String getProperty(String name) {
//                return "";
//            }
//
//            public boolean isWritePersistent() {
//                return false;
//            }
//
//            public Collection<String> getAvailablePropertyNames() {
//                return new ArrayList<String>();
//            }
//
//            public void reload() {
//                //To change body of implemented methods use File | Settings | File Templates.
//            }
//        });
//        manager.setAtlasProperties(props);
//    }
//
//    /**
//     * Checks correct workflow and logs for just one simplest (index) task
//     */
//    @Test
//    public void test_basicWorkflowForIndexTask() {
//        TaskSpec spec = new TaskSpec("index", "");
//
//        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, true, "");
//        waitForManager();
//
//        assertEquals(1, storage.taskStages.size());
//        assertTrue(storage.taskStages.containsKey(spec));
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(spec));
//
//        assertEquals(2, storage.taskLog.size());
//        assertEquals(spec, storage.taskLog.get(0).task);
//        assertEquals("INDEX", storage.taskLog.get(0).stage.toString());
//        assertEquals(TaskEvent.STARTED, storage.taskLog.get(0).event);
//
//        assertEquals(spec, storage.taskLog.get(1).task);
//        assertEquals("INDEX", storage.taskLog.get(1).stage.toString());
//        assertEquals(TaskEvent.FINISHED, storage.taskLog.get(1).event);
//
//        assertEquals(1, storage.operLog.size());
//        assertEquals(spec, storage.operLog.get(0).task);
//        assertEquals(defaultUser, storage.operLog.get(0).user);
//        assertEquals(TaskRunMode.RESTART, storage.operLog.get(0).runMode);
//        assertEquals(TaskOperation.SCHEDULE, storage.operLog.get(0).operation);
//    }
//
//    /**
//     * Checks if successful experiment build marks index as dirty
//     */
//    @Test
//    public void test_markIndexAsDirtyAfterSuccessfulExperiment() {
//        TaskSpec spec = new TaskSpec("experiment", "E-AN-1");
//        TaskSpec speci = new TaskSpec("index", "");
//
//        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, false, "");
//        waitForManager();
//
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(spec));
//        assertEquals(IndexTask.STAGE, storage.taskStages.get(speci));
//    }
//
//    /**
//     * Checks if failed experiment build DOES NOT mark index as dirty
//     */
//    @Test
//    public void test_markIndexAsDirtySometimesNot() {
//        TaskSpec spec = new TaskSpec("experiment", "E-A-1");
//        TaskSpec speci = new TaskSpec("index", "");
//
//        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, false, "");
//        waitForManager();
//
//        assertNotSame(TaskStatus.DONE, storage.taskStages.get(spec)); // failed experiment
//        assertNotSame(TaskStatus.DONE, storage.taskStages.get(speci)); // no change to index status
//    }
//
//    /**
//     * Checks correct workflow and logs for task which has auto-added dependencies
//     */
//    @Test
//    public void test_dependencyWorkflowForIndexTask() {
//        TaskSpec spec = new TaskSpec("experiment", "E-AN-1");
//
//        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, true, "");
//        waitForManager();
//
//        assertEquals(2, storage.taskStages.size());
//        assertTrue(storage.taskStages.containsKey(spec));
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(spec));
//        TaskSpec speci = new TaskSpec("index", "");
//        assertTrue(storage.taskStages.containsKey(speci));
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(speci));
//
//
//        assertEquals(4, storage.taskLog.size());
//        assertEquals(spec, storage.taskLog.get(0).task);
//        assertEquals("ANALYTICS", storage.taskLog.get(0).stage.toString());
//        assertEquals(TaskEvent.STARTED, storage.taskLog.get(0).event);
//
//        assertEquals(spec, storage.taskLog.get(1).task);
//        assertEquals("ANALYTICS", storage.taskLog.get(1).stage.toString());
//        assertEquals(TaskEvent.FINISHED, storage.taskLog.get(1).event);
//
//        assertEquals(speci, storage.taskLog.get(2).task);
//        assertEquals("INDEX", storage.taskLog.get(2).stage.toString());
//        assertEquals(TaskEvent.STARTED, storage.taskLog.get(2).event);
//
//        assertEquals(speci, storage.taskLog.get(3).task);
//        assertEquals("INDEX", storage.taskLog.get(3).stage.toString());
//        assertEquals(TaskEvent.FINISHED, storage.taskLog.get(3).event);
//
//        assertEquals(2, storage.operLog.size());
//        assertEquals(spec, storage.operLog.get(0).task);
//        assertEquals(defaultUser, storage.operLog.get(0).user);
//        assertEquals(TaskRunMode.RESTART, storage.operLog.get(0).runMode);
//        assertEquals(TaskOperation.SCHEDULE, storage.operLog.get(0).operation);
//        assertEquals(speci, storage.operLog.get(1).task);
//        assertEquals(defaultUser, storage.operLog.get(1).user);
//        assertEquals(TaskOperation.SCHEDULE, storage.operLog.get(1).operation);
//    }
//
//    /**
//     * Checks if request to continue/restart from successfully completed task works correctly
//     */
//    @Test
//    public void test_continueAndRestartFromSuccess() {
//        TaskSpec spec = new TaskSpec("experiment", "EXP-AN-1");
//        TaskSpec speci = new TaskSpec("index", "");
//
//        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, true, "");
//        waitForManager();
//        assertEquals(2, storage.operLog.size()); // 1 for experiment, 1 for auto-index
//        assertEquals(4, storage.taskLog.size()); // 4 for experiment, 2 for index
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(spec));
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(speci));
//
//        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, true, "");
//        waitForManager();
//        assertEquals(3, storage.operLog.size()); // check we've logged the request, +1 for exp, but no auto-index as no experiment actually happened
//        assertEquals(4, storage.taskLog.size()); // ...but done nothing, as it's already done (including auto-added index task!)
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(spec));
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(speci));
//
//        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, true, "");
//        waitForManager();
//        assertEquals(5, storage.operLog.size()); // check we've logged the request, +1 for exp, +1 for auto-index
//        assertEquals(8, storage.taskLog.size()); // ...and did it once again
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(spec));
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(speci));
//    }
//
//    /**
//     * ...and same for experiment, failing at one stage
//     */
//    @Test
//    public void test_continueAndRestartFromFail() throws Exception {
//        TaskSpec spec = new TaskSpec("experiment", "EXP-N-1");
//        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, false, ""); // those dependcies are in the way, so don't use them
//        waitForManager();
//        assertEquals(1, storage.operLog.size());
//        assertEquals(2, storage.taskLog.size());
//        assertEquals(TaskStatus.valueOf("ANALYTICS"), storage.taskStages.get(spec));
//
//        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, false, "");
//        waitForManager();
//        assertEquals(2, storage.operLog.size());
//        assertEquals(4, storage.taskLog.size()); // tried only analytics one more time
//        assertEquals(TaskStatus.valueOf("ANALYTICS"), storage.taskStages.get(spec)); // ...and failed once again
//
//        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, false, "");
//        waitForManager();
//        assertEquals(3, storage.operLog.size());
//        assertEquals(6, storage.taskLog.size()); // tried both stages one more time, thus +4 log entries
//        assertEquals(TaskStatus.valueOf("ANALYTICS"), storage.taskStages.get(spec)); // ...and it's still there
//
//        // MAGIC!!! it will cure failing analysis (see mock generator code)
//        manager.getAnalyticsGenerator().startup();
//
//        manager.enqueueTask(spec, TaskRunMode.CONTINUE, defaultUser, false, "");
//        waitForManager();
//        assertEquals(4, storage.operLog.size());
//        assertEquals(8, storage.taskLog.size()); // one more attempt
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(spec)); // and it should be fine now
//
//        // ...and once more from the very start to be sure it really works
//        manager.enqueueTask(spec, TaskRunMode.RESTART, defaultUser, false, "");
//        waitForManager();
//        assertEquals(5, storage.operLog.size());
//        assertEquals(10, storage.taskLog.size()); // one more attempt
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(spec)); // and it still should be fine
//    }
//
//    @Test
//    public void test_cancelPendingTask() throws Exception {
//        manager.pause();
//        TaskSpec spece1 = new TaskSpec("experiment", "EXP-AN-1");
//        manager.enqueueTask(spece1, TaskRunMode.CONTINUE, defaultUser, true, "");
//        TaskSpec specidx = new TaskSpec("index", "");
//        long taskIdxId = manager.enqueueTask(spece1, TaskRunMode.CONTINUE, defaultUser, true, "");
//        manager.start();
//        delay(); // let it do something
//        manager.cancelTask(taskIdxId, defaultUser, ""); // change our mind, cancel auto-added task
//        waitForManager();
//        assertEquals(3, storage.operLog.size()); // 2 enqueues and 1 cancel
//        assertEquals(3, storage.taskLog.size()); // first task is completed succesfully, but no trace of second task
//        assertNotSame(TaskStatus.DONE, storage.taskStages.get(spece1)); // ...and no analytics is here
//        assertNotSame(TaskStatus.DONE, storage.taskStages.get(specidx)); // indexing was not run!
//    }
//
//    @Test
//    public void test_dependenciesSequence() {
//        TaskSpec spece1 = new TaskSpec("experiment", "E-AN-1");
//        TaskSpec spece2 = new TaskSpec("experiment", "E-AN-2");
//        TaskSpec speci = new TaskSpec("index", "");
//
//        manager.pause();
//        manager.enqueueTask(spece1, TaskRunMode.CONTINUE, defaultUser, true, "");
//        manager.enqueueTask(spece2, TaskRunMode.CONTINUE, defaultUser, true, "");
//        manager.enqueueTask(speci, TaskRunMode.CONTINUE, defaultUser, true, "");
//
//        List<Task> tasks = new ArrayList<Task>(manager.getQueuedTasks());
//        assertEquals(3, tasks.size()); // 3 tasks should be here
//        assertEquals(spece1, tasks.get(1).getTaskSpec());
//        assertEquals(spece2, tasks.get(0).getTaskSpec());
//        assertEquals(speci, tasks.get(2).getTaskSpec());
//
//        manager.start();
//        waitForManager();
//        // TODO: check logs for correct running order - experiments 1 & 2 in parallel and index after
//    }
//
//    @Test
//    public void test_loadArrayDesign() {
//        TaskSpec specld = new TaskSpec("loadarraydesign", "http://host/AD-AAAA-1");
//
//        manager.enqueueTask(specld, TaskRunMode.RESTART, defaultUser, true, "");
//        waitForManager();
//
//        assertEquals(2, storage.operLog.size()); // 1 load, 1 index
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(specld));
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(new TaskSpec("index", "")));
//    }
//
//    @Test
//    public void test_loadExperiment() {
//        TaskSpec specld = new TaskSpec("loadexperiment", "http://host/E-AN-1");
//
//        manager.enqueueTask(specld, TaskRunMode.RESTART, defaultUser, true, "");
//        waitForManager();
//
//        assertEquals(3, storage.operLog.size()); // 1 load, 1 index
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(specld)); // all three should be complete
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(new TaskSpec("experiment", "E-AN-1")));
//        assertEquals(TaskStatus.DONE, storage.taskStages.get(new TaskSpec("index", "")));
//    }
//
//    @Test
//    public void test_loadExperimentFail() throws Exception {
//        TaskSpec specld = new TaskSpec("loadexperiment", "http://host/E-AN-1");
//        manager.getLoader().shutdown(); // loading will fail
//
//        manager.enqueueTask(specld, TaskRunMode.RESTART, defaultUser, true, "");
//        waitForManager();
//
//        assertEquals(1, storage.operLog.size()); // 1 load, 1 index
//        assertEquals(LoaderTask.STAGE, storage.taskStages.get(specld)); // failed
//        assertNull(storage.taskStages.get(new TaskSpec("experiment", "E-AN-1"))); // didn't run
//        assertNull(storage.taskStages.get(new TaskSpec("index", ""))); // same
//    }

}

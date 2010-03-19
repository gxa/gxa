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

package uk.ac.ebi.gxa.requesthandlers.tasks;

import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;
import uk.ac.ebi.gxa.tasks.*;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;
import uk.ac.ebi.gxa.utils.JoinIterator;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.gxa.utils.FilterIterator;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGenerator;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGenerationEvent;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGenerator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;
import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGeneratorListener;
import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGenerationEvent;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.loader.AtlasLoader;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderEvent;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.unloader.AtlasUnloader;
import uk.ac.ebi.gxa.unloader.AtlasUnloaderException;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringUtils;

/**
 * Task manager AJAX servlet
 * @author pashky
 */
public class AdminRequestHandler extends AbstractRestRequestHandler {
    private static final Map<Object,Object> EMPTY = makeMap();
    private static TaskUser defaultUser = new TaskUser("user");

    private TaskManager taskManager;
    private AtlasDAO dao;
    private DbStorage taskManagerDbStorage;
    private AtlasProperties atlasProperties;
    private static final String WEB_REQ_MESSAGE = "By web request from ";
    private static final String SESSION_ADMINUSER = "adminUserName";

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void setTaskManagerDbStorage(DbStorage dbStorage) {
        this.taskManagerDbStorage = dbStorage;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    static void delay() {
        try { Thread.sleep(1000); } catch(Exception e) {/**/}
    }

    static int DONOTHINGNUM() {
        return (int)Math.round(Math.random() * 20) + 5;
    }

    private void installTestProcessors() {
        final List<Throwable> ERRORS = Collections.<Throwable>singletonList(new Exception("Your biggest mistake"));
        taskManager.setAnalyticsGenerator(new AnalyticsGenerator() {
            boolean fixEverything = true;

            public void startup() throws AnalyticsGeneratorException {
                fixEverything = true;
            }

            public void shutdown() throws AnalyticsGeneratorException {
                fixEverything = false;
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
                        for(int i = 0; i < DONOTHINGNUM(); ++i) {
                            listener.buildProgress("Heavy analytics calculations " + i + " for " + experimentAccession);
                            delay();
                        }
                        if(fixEverything)
                            listener.buildSuccess(new AnalyticsGenerationEvent(1000, TimeUnit.MILLISECONDS));
                        else
                            listener.buildError(new AnalyticsGenerationEvent(1000, TimeUnit.MILLISECONDS, ERRORS));
                    }
                }.start();
            }
        });

        taskManager.setNetcdfGenerator(new NetCDFGenerator() {
            boolean fixEverything = true;

            public void startup() throws NetCDFGeneratorException {
                fixEverything = true;
            }

            public void shutdown() throws NetCDFGeneratorException {
                fixEverything = false;
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
                        for(int i = 0; i < DONOTHINGNUM(); ++i) {
                            listener.buildProgress("Flooding your disk with netcdfs for " + i + " for " + experimentAccession);
                            delay();
                        }
                        if(fixEverything)
                            listener.buildSuccess(new NetCDFGenerationEvent(1000, TimeUnit.MILLISECONDS));
                        else
                            listener.buildError(new NetCDFGenerationEvent(1000, TimeUnit.MILLISECONDS, ERRORS));
                    }
                }.start();
            }
        });

        taskManager.setIndexBuilder(new IndexBuilder() {
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
                        for(int i = 0; i < DONOTHINGNUM(); ++i) {
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

        taskManager.setLoader(new AtlasLoader<URL>() {
            boolean shouldFail = false;
            public void setMissingDesignElementsCutoff(double missingDesignElementsCutoff) { }
            public double getMissingDesignElementsCutoff() { return 0; }
            public void setAllowReloading(boolean allowReloading) { }
            public boolean getAllowReloading() { return false; }
            public List<String> getGeneIdentifierPriority() { return null; }
            public void setGeneIdentifierPriority(List<String> geneIdentifierPriority) { }

            public void startup() throws AtlasLoaderException {
                shouldFail = false; // a hack
            }
            public void shutdown() throws AtlasLoaderException {
                shouldFail = true; // a hack
            }

            public void loadExperiment(URL experimentResource) { }
            public void loadArrayDesign(URL arrayDesignResource) { }

            public void loadExperiment(final URL url, final AtlasLoaderListener listener) {
                log.info("Loading experiment " + url);
                new Thread() {
                    @Override
                    public void run() {
                        int cnt = DONOTHINGNUM();
                        for(int i = 0; i < cnt; ++i) {
                            listener.loadProgress("Parsing " + (i*100/ cnt) + "%");
                            delay();
                        }
                        if(shouldFail)
                            listener.loadError(AtlasLoaderEvent.error(1000, TimeUnit.MILLISECONDS, ERRORS));
                        else
                            listener.loadSuccess(AtlasLoaderEvent.success(1000, TimeUnit.MILLISECONDS,
                                    Collections.singletonList(url.getPath().substring(1))));
                    }
                }.start();
            }

            public void loadArrayDesign(final URL url, final AtlasLoaderListener listener) {
                log.info("Loading array design " + url);
                new Thread() {
                    @Override
                    public void run() {
                        int cnt = DONOTHINGNUM();
                        for(int i = 0; i < cnt; ++i) {
                            listener.loadProgress("Parsing " + (i*100/ cnt) + "%");
                            delay();
                        }
                        if(shouldFail)
                            listener.loadError(AtlasLoaderEvent.error(1000, TimeUnit.MILLISECONDS, ERRORS));
                        else
                            listener.loadSuccess(AtlasLoaderEvent.success(1000, TimeUnit.MILLISECONDS,
                                    Collections.singletonList(url.getPath().substring(1))));
                    }
                }.start();
            }
        });

        taskManager.setUnloader(new AtlasUnloader() {
            public void unloadExperiment(String accession) throws AtlasUnloaderException {
                log.info("Unloading experiment " + accession);
            }

            public void unloadArrayDesign(String accession) throws AtlasUnloaderException {
                log.info("Unloading array design " + accession);
           }
        });
    }

    public void setDao(AtlasDAO dao) {
        this.dao = dao;
    }

    private Object processPause() {
        taskManager.pause();
        return EMPTY;
    }

    private Object processRestart() {
        taskManager.start();
        return EMPTY;
    }

    private Map makeTaskObject(Task task, String state, String progress) {
        return makeMap(
                "state", state,
                "id", task.getTaskId(),
                "runMode", task.getRunMode(),
                "stage", task.getCurrentStage().toString(),
                "type", task.getTaskSpec().getType(),
                "accession", task.getTaskSpec().getAccession(),
                "progress", progress);
    }

    private Object processTaskList() {
        return makeMap(
                "isRunning", taskManager.isRunning(),
                "tasks", new JoinIterator<WorkingTask,Task,Map>(
                        taskManager.getWorkingTasks().iterator(),
                        taskManager.getQueuedTasks().iterator()
                ) {
                    public Map map1(WorkingTask task) {
                        return makeTaskObject(task, "WORKING", task.getCurrentProgress());
                    }

                    public Map map2(Task task) {
                        return makeTaskObject(task, "PENDING", null);
                    }
                });
    }

    private Object processEnqueue(String taskType, String[] accessions, String runMode, String autoDepend, String remoteId) {
        Map<String,Integer> result = new HashMap<String, Integer>();
        boolean wasRunning = taskManager.isRunning();
        if(wasRunning)
            taskManager.pause();
        for(String accession : accessions) {
            int id = taskManager.enqueueTask(new TaskSpec(taskType, accession),
                    TaskRunMode.valueOf(runMode),
                    defaultUser,
                    toBoolean(autoDepend),
                    WEB_REQ_MESSAGE + remoteId);
            result.put(accession,  id);
        }
        if(wasRunning)
            taskManager.start(); // TODO: should make batch adds here, huh?
        return result;
    }

    private static boolean toBoolean(String stringValue) {
        return "1".equals(stringValue) || "true".equalsIgnoreCase(stringValue) || "yes".equalsIgnoreCase(stringValue);
    }

    private Object processCancel(String[] taskIds, String remoteId) {
        for(String taskId : taskIds)
            taskManager.cancelTask(Integer.valueOf(taskId), defaultUser, WEB_REQ_MESSAGE + remoteId);
        return EMPTY;
    }

    private Object processCancelAll(String remoteId) {
        taskManager.cancelAllTasks(defaultUser, WEB_REQ_MESSAGE + remoteId);
        return EMPTY;
    }

    private Object processGetStage(String taskType, String accession) {
        TaskStage stage = taskManager.getTaskStage(new TaskSpec(taskType, accession));
        return makeMap("stage", stage.toString());
    }

    private Object processEnqueueSearchExperiments(String type,
                                                   String searchText, String fromDate, String toDate, String pendingOnlyStr,
                                                   String runMode, String autoDepend, String remoteId) {
        Map<String,Integer> result = new HashMap<String, Integer>();
        boolean wasRunning = taskManager.isRunning();
        if(wasRunning)
            taskManager.pause();
        for(Iterator<Pair<Experiment, TaskStage>> i = getSearchExperiments(searchText, fromDate, toDate, pendingOnlyStr); i.hasNext();) {
            Experiment experiment = i.next().getFirst();
            int id = taskManager.enqueueTask(new TaskSpec(type, experiment.getAccession()),
                    TaskRunMode.valueOf(runMode),
                    defaultUser,
                    toBoolean(autoDepend), WEB_REQ_MESSAGE + remoteId);
            result.put(experiment.getAccession(),  id);
        }        
        if(wasRunning)
            taskManager.start(); // TODO: should make batch adds here, huh?
        return result;
    }

    private Object processSearchExperiments(String searchText, String fromDate, String toDate, String pendingOnlyStr) {
        List<Map> results = new ArrayList<Map>();
        int numCollapsed = 0;
        for(Iterator<Pair<Experiment, TaskStage>> i = getSearchExperiments(searchText, fromDate, toDate, pendingOnlyStr); i.hasNext();) {
            Pair<Experiment, TaskStage> e = i.next();
            if(results.size() < 20)
                results.add(makeMap(
                        "accession", e.getFirst().getAccession(),
                        "stage", e.getSecond().toString(),
                        "loadDate", IN_DATE_FORMAT.format(e.getFirst().getLoadDate())));
            else
                ++numCollapsed;
        }
        return makeMap(
                "experiments", results,
                "numCollapsed", numCollapsed,
                "numTotal", numCollapsed + results.size(),
                "indexStage", taskManager.getTaskStage(new TaskSpec(IndexTask.TYPE, "")).toString()
                );
    }

    private Iterator<Pair<Experiment,TaskStage>> getSearchExperiments(String searchTextStr,
                                                                  String fromDateStr, String toDateStr,
                                                                  String pendingOnlyStr) {
        final String searchText = searchTextStr.toLowerCase();
        final boolean pendingOnly = toBoolean(pendingOnlyStr);

        final Date fromDate = parseDate(fromDateStr);
        final Date toDate = parseDate(toDateStr);

        List<Experiment> experiments = dao.getExperimentByLoadDate(fromDate, toDate);

        return new FilterIterator<Experiment, Pair<Experiment, TaskStage>>(experiments.iterator()) {
            @Override
            public Pair<Experiment, TaskStage> map(Experiment experiment) {
                final TaskStage stage = taskManager.getTaskStage(new TaskSpec(ExperimentTask.TYPE, experiment.getAccession()));
                boolean searchYes = "".equals(searchText)
                        || experiment.getAccession().toLowerCase().contains(searchText)
                        || experiment.getDescription().toLowerCase().contains(searchText);
                boolean pendingYes = !pendingOnly
                        || !TaskStage.DONE.equals(stage);
                return searchYes && pendingYes ? new Pair<Experiment, TaskStage>(experiment, stage) : null;
            }
        };
    }

    private Date parseDate(String toDateStr) {
        try {
            return IN_DATE_FORMAT.parse(toDateStr);
        } catch(Exception e) {
            return null;
        }
    }

    private static SimpleDateFormat OUT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static SimpleDateFormat IN_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private static String formatTimeStamp(Timestamp ts) {
        return OUT_DATE_FORMAT.format(ts);
    }

    private Object processOperationLog(String numStr) {
        int num = Integer.valueOf(numStr);
        return makeMap("items", new MappingIterator<DbStorage.OperationLogItem, Map>(taskManagerDbStorage.getLastOperationLogItems(num).iterator()) {
            public Map map(DbStorage.OperationLogItem li) {
                return makeMap(
                        "runMode", li.runMode,
                        "operation", li.operation,
                        "type", li.taskSpec.getType(),
                        "accession", li.taskSpec.getAccession(),
                        "user", li.user.getUserName(),
                        "message", li.message,
                        "time", formatTimeStamp(li.timestamp)
                );
            }
        });
    }

    private Object processTaskEventLog(String numStr) {
        int num = Integer.valueOf(numStr);
        return makeMap("items", new MappingIterator<DbStorage.TaskEventLogItem, Map>(taskManagerDbStorage.getLastTaskEventLogItems(num).iterator()) {
            public Map map(DbStorage.TaskEventLogItem li) {
                return makeMap(
                        "type", li.taskSpec.getType(),
                        "accession", li.taskSpec.getAccession(),
                        "message", li.message,
                        "stage", li.stage.getStage(),
                        "event", li.event,
                        "time", formatTimeStamp(li.timestamp)
                );
            }
        });
    }

    private Object processLoadList() {
        return makeMap(
                "experiments",
                new MappingIterator<Map.Entry<TaskSpec,TaskStage>, Map>(taskManagerDbStorage.getTaskStagesByType("loadexperiment").entrySet().iterator()) {
                    public Map map(Map.Entry<TaskSpec, TaskStage> load) {
                        return makeMap("url", load.getKey().getAccession(), "done", TaskStage.DONE.equals(load.getValue()));
                    }
                },

                "arraydesigns",
                new MappingIterator<Map.Entry<TaskSpec,TaskStage>, Map>(taskManagerDbStorage.getTaskStagesByType("loadarraydesign").entrySet().iterator()) {
                    public Map map(Map.Entry<TaskSpec, TaskStage> load) {
                        return makeMap("url", load.getKey().getAccession(), "done", TaskStage.DONE.equals(load.getValue()) ? "1" : null);
                    }
                }
        );
    }

    private Object processPropertyList() {
        List<String> names = new ArrayList<String>(atlasProperties.getAvailablePropertyNames());
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return makeMap("properties", new MappingIterator<String,Map>(names.iterator()) {
            public Map map(String name) {
                return makeMap("name", name, "value", atlasProperties.getProperty(name));
            }
        });
    }

    private Object processPropertySet(Map<String,String[]> paramMap) {
        Collection<String> names = atlasProperties.getAvailablePropertyNames();
        for(Map.Entry<String,String[]> e : paramMap.entrySet()) {
            if(names.contains(e.getKey())) {
                String newValue = StringUtils.join(e.getValue(), ",");
                atlasProperties.setProperty(e.getKey(), "".equals(newValue) ? null : newValue);
            }
        }
        return EMPTY;
    }

    public TaskUser checkLogin(String username, String password) {
        if(username != null && username.matches(".*\\S.*") && "password".equals(password)) {
            return new TaskUser(username);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object process(HttpServletRequest request) {

//        installTestProcessors();
        String op = request.getParameter("op");

        String remoteId = request.getRemoteHost();
        if(remoteId == null || "".equals(remoteId))
            remoteId = request.getRemoteAddr();
        if(remoteId == null || "".equals(remoteId))
            remoteId = "unknown";

//        HttpSession session = request.getSession(true);
//
//        TaskUser authenticatedUser = null;
//        if("login".equals(op)) {
//            authenticatedUser = checkLogin(request.getParameter("userName"), request.getParameter("password"));
//            if(authenticatedUser == null) {
//                return makeMap("error", "Not authenticated", "notAuthenticated", true);
//            }
//            request.setAttribute(SESSION_ADMINUSER, authenticatedUser);
//            return makeMap("success", true);
//        } else if(session.getAttribute(SESSION_ADMINUSER) == null ) {
//            return makeMap("error", "Not authenticated", "notAuthenticated", true);
//        }

        if("pause".equals(op))
            return processPause();

        else if("restart".equals(op))
            return processRestart();

        else if("tasklist".equals(op))
            return processTaskList();

        else if("enqueue".equals(op))
            return processEnqueue(
                    request.getParameter("type"),
                    request.getParameterValues("accession"),
                    request.getParameter("runMode"),
                    request.getParameter("autoDepends"),
                    remoteId);

        else if("cancel".equals(op))
            return processCancel(request.getParameterValues("id"),
                    remoteId);

        else if("cancelall".equals(op))
            return processCancelAll(remoteId);

        else if("getstage".equals(op))
            return processGetStage(
                    request.getParameter("type"),
                    request.getParameter("accession"));

        else if("searchexp".equals(op))
            return processSearchExperiments(
                    request.getParameter("search"),
                    request.getParameter("fromDate"),
                    request.getParameter("toDate"),
                    request.getParameter("pendingOnly"));

        else if("enqueuesearchexp".equals(op))
            return processEnqueueSearchExperiments(
                    request.getParameter("type"),
                    request.getParameter("search"),
                    request.getParameter("fromDate"),
                    request.getParameter("toDate"),
                    request.getParameter("pendingOnly"),
                    request.getParameter("runMode"),
                    request.getParameter("autoDepends"),
                    remoteId);

        else if("operlog".equals(op))
            return processOperationLog(request.getParameter("num"));

        else if("tasklog".equals(op))
            return processTaskEventLog(request.getParameter("num"));

        else if("loadlist".equals(op))
            return processLoadList();

        else if("proplist".equals(op))
            return processPropertyList();

        else if("propset".equals(op))
            return processPropertySet((Map<String,String[]>)request.getParameterMap());

        return new ErrorResult("Unknown operation specified: " + op);
    }
}

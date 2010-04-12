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

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;
import uk.ac.ebi.gxa.tasks.*;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;
import static uk.ac.ebi.gxa.utils.CollectionUtil.addMap;
import uk.ac.ebi.gxa.utils.FilterIterator;
import uk.ac.ebi.gxa.utils.JoinIterator;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGenerator;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGenerationEvent;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Task manager AJAX servlet
 * @author pashky
 */
public class AdminRequestHandler extends AbstractRestRequestHandler {
    private static final Map<Object,Object> EMPTY = makeMap();

    private TaskManager taskManager;
    private AtlasDAO dao;
    private DbStorage taskManagerDbStorage;
    private AtlasProperties atlasProperties;
    private static final String WEB_REQ_MESSAGE = "By web request from ";
    private static final String SESSION_ADMINUSER = "adminUserName";
    private static SimpleDateFormat OUT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static SimpleDateFormat IN_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");


    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void setTaskManagerDbStorage(DbStorage dbStorage) {
        this.taskManagerDbStorage = dbStorage;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
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

    private Map<String,String> makeTaskObject(Task task, String state) {
        return makeMap(
                "state", state,
                "id", task.getTaskId(),
                "runMode", task.getRunMode(),
                "type", task.getTaskSpec().getType(),
                "user", task.getUser().getUserName(),
                "accession", task.getTaskSpec().getAccession());
    }

    private Object processTaskList(String pageStr, String numStr) {
        int page = Math.max(0, Integer.valueOf(pageStr));
        int num = Math.max(1, Integer.valueOf(numStr));

        List<WorkingTask> working = taskManager.getWorkingTasks();
        List<Task> pending = taskManager.getQueuedTasks();

        int wsize = working.size();
        int psize = pending.size();

        int from = page * num;
        if(wsize + psize > 0 && from >= wsize + psize) {
            page = (wsize + psize - 1) / num;
            from = page * num;
        }

        working = working.subList(
                Math.min(from, wsize),
                Math.min(from + num, wsize));
        pending = pending.subList(
                Math.min(Math.max(0, from - wsize), psize),
                Math.min(Math.max(0, from + num - wsize), psize));

        return makeMap(
                "isRunning", taskManager.isRunning(),
                "start", from,
                "page", page,
                "numTotal", wsize + psize,
                "numWorking", wsize,
                "numPending", psize,
                "tasks", new JoinIterator<WorkingTask,Task,Map>(
                        working.iterator(),
                        pending.iterator()
                ) {
                    public Map map1(WorkingTask task) {
                        long elapsedTime = task.getElapsedTime() / 1000;
                        return addMap(makeTaskObject(task, "WORKING"),
                                "progress", task.getCurrentProgress(),
                                "elapsed", String.format("%d:%02d:%02d",
                                        elapsedTime / 3600, (elapsedTime % 3600) / 60, elapsedTime % 60));
                    }

                    public Map map2(Task task) {
                        return makeTaskObject(task, "PENDING");
                    }
                });
    }

    private Object processSchedule(String taskType, String[] accessions, String runMode, String autoDepend, String remoteId, TaskUser user) {
        Map<String,Long> result = new HashMap<String, Long>();
        boolean wasRunning = taskManager.isRunning();
        if(wasRunning)
            taskManager.pause();
        for(String accession : accessions) {
            long id = taskManager.scheduleTask(new TaskSpec(taskType, accession),
                    TaskRunMode.valueOf(runMode),
                    user,
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

    private Object processCancel(String[] taskIds, String remoteId, TaskUser user) {
        for(String taskId : taskIds)
            taskManager.cancelTask(Integer.valueOf(taskId), user, WEB_REQ_MESSAGE + remoteId);
        return EMPTY;
    }

    private Object processCancelAll(String remoteId, TaskUser user) {
        taskManager.cancelAllTasks(user, WEB_REQ_MESSAGE + remoteId);
        return EMPTY;
    }

    private Object processGetStage(String taskType, String accession) {
        TaskStatus stage = taskManager.getTaskStage(new TaskSpec(taskType, accession));
        return makeMap("stage", stage.toString());
    }

    private Object processScheduleSearchExperiments(String type,
                                                   String searchText, String fromDate, String toDate, String pendingOnlyStr,
                                                   String runMode, String autoDepend, String remoteId, TaskUser user) {
        Map<String,Long> result = new HashMap<String, Long>();
        boolean wasRunning = taskManager.isRunning();
        if(wasRunning)
            taskManager.pause();
        for(Iterator<Pair<Experiment, TaskStatus>> i = getSearchExperiments(searchText, fromDate, toDate, pendingOnlyStr); i.hasNext();) {
            Experiment experiment = i.next().getFirst();
            long id = taskManager.scheduleTask(new TaskSpec(type, experiment.getAccession()),
                    TaskRunMode.valueOf(runMode),
                    user,
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
        for(Iterator<Pair<Experiment, TaskStatus>> i = getSearchExperiments(searchText, fromDate, toDate, pendingOnlyStr); i.hasNext();) {
            Pair<Experiment, TaskStatus> e = i.next();
            if(results.size() < 20)
                results.add(makeMap(
                        "accession", e.getFirst().getAccession(),
                        "description", e.getFirst().getDescription(),
                        "analytics", e.getSecond().toString(),
                        "loadDate", e.getFirst().getLoadDate() != null ? IN_DATE_FORMAT.format(e.getFirst().getLoadDate()) : "unknown"));
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

    private Iterator<Pair<Experiment, TaskStatus>> getSearchExperiments(String searchTextStr,
                                                                  String fromDateStr, String toDateStr,
                                                                  String pendingOnlyStr) {
        final String searchText = searchTextStr.toLowerCase();
        final boolean pendingOnly = toBoolean(pendingOnlyStr);

        final Date fromDate = parseDate(fromDateStr);
        final Date toDate = parseDate(toDateStr);

        List<Experiment> experiments = dao.getExperimentByLoadDate(fromDate, toDate);

        return new FilterIterator<Experiment, Pair<Experiment, TaskStatus>>(experiments.iterator()) {
            @Override
            public Pair<Experiment, TaskStatus> map(Experiment experiment) {
                final TaskStatus analyticsState = taskManager.getTaskStage(new TaskSpec(AnalyticsTask.TYPE, experiment.getAccession()));
                boolean searchYes = "".equals(searchText)
                        || experiment.getAccession().toLowerCase().contains(searchText)
                        || experiment.getDescription().toLowerCase().contains(searchText);
                boolean pendingYes = !pendingOnly
                        || !TaskStatus.DONE.equals(analyticsState);
                return searchYes && pendingYes ? new Pair<Experiment, TaskStatus>(experiment, analyticsState) : null;
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

    private static String formatTimeStamp(Timestamp ts) {
        return OUT_DATE_FORMAT.format(ts);
    }

    private class TaskEventLogMapper extends MappingIterator<DbStorage.TaskEventLogItem, Map> {
        private TaskEventLogMapper(Iterator<DbStorage.TaskEventLogItem> fromiter) {
            super(fromiter);
        }

        public Map map(DbStorage.TaskEventLogItem li) {
            return makeMap(
                    "type", li.taskSpec.getType(),
                    "accession", li.taskSpec.getAccession(),
                    "message", li.message,
                    "user", li.user.getUserName(),
                    "runMode", li.runMode,
                    "event", li.event,
                    "time", formatTimeStamp(li.timestamp)
            );
        }
    }

    private Object processTaskEventLog(String numStr) {
        int num = Integer.valueOf(numStr);
        return makeMap("items", new TaskEventLogMapper(taskManagerDbStorage.getLastTaskEventLogItems(num).iterator()));
    }

    private Object processExperimentTaskEventLog(String accession) {
        return makeMap("items", new TaskEventLogMapper(taskManagerDbStorage.getExperimentHistory(accession).iterator()));
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
        if(username != null && username.matches(".*\\S{3,}.*") && "password".equals(password)) {
            return new TaskUser(username);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object process(HttpServletRequest request) {

//        taskManager.setIndexBuilder(new IndexBuilder() {
//            public void setIncludeIndexes(List<String> includeIndexes) {
//            }
//            public List<String> getIncludeIndexes() {
//                return null;
//            }
//            public void startup() throws IndexBuilderException {
//            }
//            public void shutdown() throws IndexBuilderException {
//            }
//            public void buildIndex() {
//            }
//            public void buildIndex(final IndexBuilderListener listener) {
//                new Thread() {
//                    @Override
//                    public void run() {
//                        for(int i = 0; i < 10; ++i) {
//                            log.info("Index building " + i);
//                            listener.buildProgress("Index building " + i);
//                            try { Thread.sleep(1000); } catch (Exception e) {}
//                        }
//                        listener.buildSuccess(new IndexBuilderEvent(1000, TimeUnit.MILLISECONDS));
//                    }
//                }.start();
//            }
//            public void registerIndexBuildEventHandler(IndexBuilderEventHandler handler) {
//            }
//            public void unregisterIndexBuildEventHandler(IndexBuilderEventHandler handler) {
//            }
//        });
//
//        taskManager.setAnalyticsGenerator(new AnalyticsGenerator() {
//            public void startup() throws AnalyticsGeneratorException {
//            }
//            public void shutdown() throws AnalyticsGeneratorException {
//            }
//            public void generateAnalytics() {
//            }
//            public void generateAnalytics(AnalyticsGeneratorListener listener) {
//            }
//            public void generateAnalyticsForExperiment(String experimentAccession) {
//            }
//            public void generateAnalyticsForExperiment(String experimentAccession, final AnalyticsGeneratorListener listener) {
//                new Thread() {
//                    @Override
//                    public void run() {
//                        for(int i = 0; i < 4; ++i) {
//                            log.info("Analytics " + i);
//                            listener.buildProgress("Analytics " + i);
//                            try { Thread.sleep(1000); } catch (Exception e) {}
//                        }
//                        listener.buildSuccess(new AnalyticsGenerationEvent(1000, TimeUnit.MILLISECONDS));
//                    }
//                }.start();
//            }
//        });
//
        String op = request.getParameter("op");

        String remoteId = request.getRemoteHost();
        if(remoteId == null || "".equals(remoteId))
            remoteId = request.getRemoteAddr();
        if(remoteId == null || "".equals(remoteId))
            remoteId = "unknown";

        HttpSession session = request.getSession(true);

        TaskUser authenticatedUser = (TaskUser)session.getAttribute(SESSION_ADMINUSER);
        if("login".equals(op)) {
            authenticatedUser = checkLogin(request.getParameter("userName"), request.getParameter("password"));
            if(authenticatedUser == null) {
                return EMPTY;
            }
            log.info("Authenticated as user " + authenticatedUser);

            session.setAttribute(SESSION_ADMINUSER, authenticatedUser);
            return makeMap("success", true, "userName", authenticatedUser.getUserName());
        } else if(authenticatedUser == null) {
            return makeMap("notAuthenticated", true);
        }

        if("pause".equals(op))
            return processPause();

        else if("restart".equals(op))
            return processRestart();

        else if("tasklist".equals(op))
            return processTaskList(request.getParameter("p"), request.getParameter("n"));

        else if("schedule".equals(op))
            return processSchedule(
                    request.getParameter("type"),
                    request.getParameterValues("accession"),
                    request.getParameter("runMode"),
                    request.getParameter("autoDepends"),
                    remoteId,
                    authenticatedUser);

        else if("cancel".equals(op))
            return processCancel(request.getParameterValues("id"),
                    remoteId,
                    authenticatedUser);

        else if("cancelall".equals(op))
            return processCancelAll(remoteId, authenticatedUser);

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

        else if("schedulesearchexp".equals(op))
            return processScheduleSearchExperiments(
                    request.getParameter("type"),
                    request.getParameter("search"),
                    request.getParameter("fromDate"),
                    request.getParameter("toDate"),
                    request.getParameter("pendingOnly"),
                    request.getParameter("runMode"),
                    request.getParameter("autoDepends"),
                    remoteId,
                    authenticatedUser);

        else if("tasklog".equals(op))
            return processTaskEventLog(request.getParameter("num"));

        else if("tasklogexp".equals(op))
            return processExperimentTaskEventLog(request.getParameter("accession"));

        else if("proplist".equals(op))
            return processPropertyList();

        else if("propset".equals(op))
            return processPropertySet((Map<String,String[]>)request.getParameterMap());

        else if("logout".equals(op)) {
            session.removeAttribute(SESSION_ADMINUSER);
            return EMPTY;
        }
        else if("getuser".equals(op))
            return makeMap("userName", authenticatedUser.getUserName());

        return new ErrorResult("Unknown operation specified: " + op);
    }
}

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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.jmx.AtlasManager;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RequestWrapper;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;
import uk.ac.ebi.gxa.tasks.*;
import uk.ac.ebi.gxa.utils.JoinIterator;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import static uk.ac.ebi.gxa.utils.CollectionUtil.addMap;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

/**
 * Task manager AJAX servlet. Mainly handles requests to TaskManager but also tracks user authentications, system
 * information display and atlas properties displays and changes
 *
 * @author pashky
 */
public class AdminRequestHandler extends AbstractRestRequestHandler {
    private static final Map<Object, Object> EMPTY = Collections.emptyMap();

    private TaskManager taskManager;
    private AtlasDAO dao;
    private DbStorage taskManagerDbStorage;
    private AtlasProperties atlasProperties;
    private static final String WEB_REQ_MESSAGE = "By web request from ";
    private static final String SESSION_ADMINUSER = "adminUserName";
    private static SimpleDateFormat OUT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static SimpleDateFormat IN_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private AtlasManager atlasManager;

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

    public void setAtlasManager(AtlasManager manager) {
        this.atlasManager = manager;
    }

    private Object processPause() {
        taskManager.pause();
        return EMPTY;
    }

    private Object processRestart() {
        taskManager.start();
        return EMPTY;
    }

    private Map<String, String> makeTaskObject(Task task, String state) {
        return makeMap(
                "state", state,
                "id", task.getTaskId(),
                "runMode", task.getRunMode(),
                "type", task.getTaskSpec().getType(),
                "user", task.getUser().getUserName(),
                "accession", task.getTaskSpec().getAccession());
    }

    private Object processTaskList(int page, int num) {

        List<WorkingTask> working = taskManager.getWorkingTasks();
        List<Task> pending = taskManager.getQueuedTasks();

        int wsize = working.size();
        int psize = pending.size();

        int from = page * num;
        if (wsize + psize > 0 && from >= wsize + psize) {
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
                "tasks", new JoinIterator<WorkingTask, Task, Map>(
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

    private Map<String, Long> processSchedule(String taskType, String[] accessions, String runMode, boolean autoDepend, String remoteId, TaskUser user, Multimap<String, String> userData) {
        Map<String, Long> result = new HashMap<String, Long>();
        boolean wasRunning = taskManager.isRunning();
        if (wasRunning)
            taskManager.pause();
        for (String accession : accessions) {
            long id = taskManager.scheduleTask(new TaskSpec(taskType, accession, userData),
                    TaskRunMode.valueOf(runMode),
                    user,
                    autoDepend,
                    WEB_REQ_MESSAGE + remoteId);
            result.put(accession, id);
        }
        if (wasRunning)
            taskManager.start(); // TODO: should make batch adds here, huh?
        return result;
    }

    private Object processCancel(String[] taskIds, String remoteId, TaskUser user) {
        for (String taskId : taskIds)
            taskManager.cancelTask(Integer.valueOf(taskId), user, WEB_REQ_MESSAGE + remoteId);
        // TODO: we do need to make sure the task was in fact cancelled
        return EMPTY;
    }

    private Object processCancelAll(String remoteId, TaskUser user) {
        taskManager.cancelAllTasks(user, WEB_REQ_MESSAGE + remoteId);
        // TODO: we do need to make sure the tasks were in fact cancelled
        return EMPTY;
    }

    private Object processScheduleSearchExperiments(String type,
                                                    String searchText, Date fromDate, Date toDate,
                                                    DbStorage.ExperimentIncompleteness incompleteness,
                                                    String runMode, boolean autoDepend,
                                                    String remoteId, TaskUser user) {

        Map<String, Long> result = new HashMap<String, Long>();
        boolean wasRunning = taskManager.isRunning();
        if (wasRunning)
            taskManager.pause();
        for (Experiment experiment : taskManagerDbStorage.findExperiments(searchText, fromDate, toDate, incompleteness, 0, -1)) {
            long id = taskManager.scheduleTask(new TaskSpec(type, experiment.getAccession(), HashMultimap.<String, String>create()),
                    TaskRunMode.valueOf(runMode),
                    user,
                    autoDepend, WEB_REQ_MESSAGE + remoteId);
            result.put(experiment.getAccession(), id);
        }
        if (wasRunning)
            taskManager.start(); // TODO: should make batch adds here, huh?
        return result;
    }

    private Object processSearchExperiments(String searchText, Date fromDate, Date toDate,
                                            DbStorage.ExperimentIncompleteness incompleteness,
                                            int page, int num) {
        int from = page * num;
        DbStorage.ExperimentList experiments = taskManagerDbStorage.findExperiments(searchText, fromDate, toDate, incompleteness, from, num);

        return makeMap(
                "experiments", new MappingIterator<DbStorage.ExperimentWithStatus, Map>(experiments.iterator()) {
                    public Map map(DbStorage.ExperimentWithStatus e) {
                        return makeMap(
                                "accession", e.getAccession(),
                                "description", e.getDescription(),
                                "numassays", dao.getCountAssaysForExperimentID(e.getExperimentID()),
                                "analytics", e.isAnalyticsComplete(),
                                "netcdf", e.isNetcdfComplete(),
                                "index", e.isIndexComplete(),
                                "private", e.isPrivate(),
                                "curated", e.isCurated(),
                                "loadDate", e.getLoadDate() != null ? IN_DATE_FORMAT.format(e.getLoadDate()) : "unknown"
                        );
                    }
                },
                "page", page,
                "numTotal", experiments.getNumTotal(),
                "indexStatus", !taskManagerDbStorage.isAnyIncomplete(IndexTask.TYPE_INDEX, IndexTask.TYPE_INDEXEXPERIMENT)
        );
    }

    private Object processGetMaxReleaseDate() {
        return taskManagerDbStorage.getMaxReleaseDate();
    }

    private Object processSearchArrayDesigns(String search, int page, int num) {
        search = search.toLowerCase();
        List<Map> results = new ArrayList<Map>();

        int from = page * num;
        int total = 0;
        for (ArrayDesign arrayDesign : dao.getAllArrayDesigns())
            if ("".equals(search)
                    || arrayDesign.getAccession().toLowerCase().contains(search)
                    || StringUtils.trimToEmpty(arrayDesign.getName()).toLowerCase().contains(search)
                    || StringUtils.trimToEmpty(arrayDesign.getProvider()).toLowerCase().contains(search)
                    ) {
                if (total >= from && total < from + num)
                    results.add(makeMap(
                            "accession", arrayDesign.getAccession(),
                            "provider", arrayDesign.getProvider(),
                            "description", arrayDesign.getName()));

                ++total;
            }

        return makeMap("arraydesigns", results, "page", page, "numTotal", total);
    }

    private Date parseDate(String toDateStr) {
        try {
            return IN_DATE_FORMAT.parse(StringUtils.trimToNull(toDateStr));
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatTimeStamp(Timestamp ts) {
        return OUT_DATE_FORMAT.format(ts);
    }

    private static class TaskEventLogMapper extends MappingIterator<DbStorage.TaskEventLogItem, Map> {
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
                    "timestamp", li.timestamp.getTime(),
                    "time", formatTimeStamp(li.timestamp)
            );
        }
    }

    private Object processTaskEventLog(TaskEvent eventFilter, String userFilter,
                                       String typeFilter, String accessionFilter,
                                       int page, int num) {
        int start = page * num;
        DbStorage.TaskEventLogItemList result = taskManagerDbStorage.findTaskLogItems(
                eventFilter,
                StringUtils.trimToNull(userFilter) == null ? null : new TaskUser(userFilter),
                StringUtils.trimToNull(typeFilter),
                StringUtils.trimToNull(accessionFilter),
                start, num);
        return makeMap("items", new TaskEventLogMapper(result.iterator()),
                "numTotal", result.getNumTotal(),
                "page", result.getStart() / num,
                "typeFacet", result.getTypeFacet(),
                "eventFacet", result.getEventFacet(),
                "userFacet", result.getUserNameFacet());
    }

    private Object processExperimentTaskEventLog(TaskTagType tagtype, String accession) {
        return makeMap("items", new TaskEventLogMapper(taskManagerDbStorage.getTaggedHistory(tagtype, accession).iterator()));
    }

    private Object processPropertyList() {
        List<String> names = new ArrayList<String>(atlasProperties.getAvailablePropertyNames());
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return makeMap("properties", new MappingIterator<String, Map>(names.iterator()) {
            public Map map(String name) {
                return makeMap("name", name, "value", atlasProperties.getProperty(name));
            }
        });
    }

    private Object processPropertySet(Map<String, String[]> paramMap) {
        Collection<String> names = atlasProperties.getAvailablePropertyNames();
        for (Map.Entry<String, String[]> e : paramMap.entrySet()) {
            if (names.contains(e.getKey())) {
                String newValue = StringUtils.join(e.getValue(), ",");
                atlasProperties.setProperty(e.getKey(), "".equals(newValue) ? null : newValue);
            }
        }
        return EMPTY;
    }

    private Object processAboutSystem() {
        return makeMap(
                "dbUrl", atlasManager.getDataSourceURL(),
                "pathData", atlasManager.getDataPath(),
                "pathIndex", atlasManager.getIndexPath(),
                "pathWebapp", atlasManager.getWebappPath(),
                "efo", atlasManager.getEFO()
        );
    }

    public TaskUser checkLogin(String username, String password) {
        if (username != null && username.matches(".*\\S{3,}.*") && password.equals(atlasProperties.getProperty("atlas.admin.password"))) {
            return new TaskUser(username);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object process(HttpServletRequest request) {
        RequestWrapper req = new RequestWrapper(request);

        String op = req.getStr("op");

        String remoteId = req.getRemoteHost();

        HttpSession session = req.getSession(true);

        TaskUser authenticatedUser = (TaskUser) session.getAttribute(SESSION_ADMINUSER);
        if ("login".equals(op)) {
            authenticatedUser = checkLogin(req.getStr("userName"), req.getStr("password"));
            if (authenticatedUser == null) {
                return EMPTY;
            }
            log.info("Authenticated as user " + authenticatedUser);

            session.setAttribute(SESSION_ADMINUSER, authenticatedUser);
            return makeMap("success", true, "userName", authenticatedUser.getUserName());
        } else if (authenticatedUser == null) {
            return makeMap("notAuthenticated", true);
        }

        if ("pause".equals(op))
            return processPause();

        else if ("restart".equals(op))
            return processRestart();

        else if ("tasklist".equals(op))
            return processTaskList(req.getInt("p"), req.getInt("n", 1, 1));

        else if ("schedule".equals(op))
            return processSchedule(
                    req.getStr("type"),
                    req.getStrArray("accession"),
                    req.getStr("runMode"),
                    req.getBool("autoDepends"),
                    remoteId,
                    authenticatedUser,
                    req.getMultimap());

        else if ("cancel".equals(op))
            return processCancel(req.getStrArray("id"),
                    remoteId,
                    authenticatedUser);

        else if ("cancelall".equals(op))
            return processCancelAll(remoteId, authenticatedUser);

        else if ("searchexp".equals(op))
            return processSearchExperiments(
                    req.getStr("search"),
                    parseDate(req.getStr("fromDate")),
                    parseDate(req.getStr("toDate")),
                    req.getEnum("pendingOnly", DbStorage.ExperimentIncompleteness.ALL),
                    req.getInt("p", 0, 0),
                    req.getInt("n", 1, 1));

        else if ("maxreleasedate".equals(op))
            return processGetMaxReleaseDate();

        else if ("searchad".equals(op))
            return processSearchArrayDesigns(
                    req.getStr("search"),
                    req.getInt("p", 0, 0),
                    req.getInt("n", 1, 1));

        else if ("schedulesearchexp".equals(op))
            return processScheduleSearchExperiments(
                    req.getStr("type"),
                    req.getStr("search"),
                    parseDate(req.getStr("fromDate")),
                    parseDate(req.getStr("toDate")),
                    req.getEnum("pendingOnly", DbStorage.ExperimentIncompleteness.ALL),
                    req.getStr("runMode"),
                    req.getBool("autoDepends"),
                    remoteId,
                    authenticatedUser);

        else if ("tasklog".equals(op))
            return processTaskEventLog(
                    req.getEnumNullDefault("event", TaskEvent.class),
                    req.getStr("user"),
                    req.getStr("type"),
                    req.getStr("accession"),
                    req.getInt("p", -1, -1),
                    req.getInt("n", 1, 1));

        else if ("tasklogtag".equals(op))
            return processExperimentTaskEventLog(req.getEnum("type", TaskTagType.EXPERIMENT), req.getStr("accession"));

        else if ("proplist".equals(op))
            return processPropertyList();

        else if ("propset".equals(op))
            return processPropertySet(req.getMap());

        else if ("aboutsys".equals(op))
            return processAboutSystem();

        else if ("logout".equals(op)) {
            session.removeAttribute(SESSION_ADMINUSER);
            return EMPTY;
        } else if ("getuser".equals(op))
            return makeMap("userName", authenticatedUser.getUserName());

        return new ErrorResult("Unknown operation specified: " + op);
    }
}

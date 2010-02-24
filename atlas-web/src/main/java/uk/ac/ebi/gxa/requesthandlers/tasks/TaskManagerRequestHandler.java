package uk.ac.ebi.gxa.requesthandlers.tasks;

import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;
import uk.ac.ebi.gxa.tasks.*;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;
import uk.ac.ebi.gxa.utils.JoinIterator;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Task manager AJAX servlet
 * @author pashky
 */
public class TaskManagerRequestHandler extends AbstractRestRequestHandler {
    private static final Map<Object,Object> EMPTY = makeMap();
    private static TaskUser defaultUser = new TaskUser("user");

    private TaskManager taskManager;
    private AtlasDAO dao;

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
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

    private Map makeTaskObject(Task task, String state) {
        return makeMap(
                "state", state,
                "id", task.getTaskId(),
                "runMode", task.getRunMode(),
                "stage", task.getCurrentStage().toString(),
                "type", task.getTaskSpec().getType(),
                "accession", task.getTaskSpec().getAccession());
    }

    private Object processTaskList() {
        return makeMap("tasks",
                new JoinIterator<Task,Task,Map>(
                        taskManager.getWorkingTasks().iterator(),
                        taskManager.getQueuedTasks().iterator()
                ) {
                    public Map map1(Task task) {
                        return makeTaskObject(task, "WORKING");
                    }

                    public Map map2(Task task) {
                        return makeTaskObject(task, "PENDING");
                    }
                });
    }

    private Object processEnqueue(String taskType, String accession, String runMode, String autoDepend) {
        int id = taskManager.enqueueTask(new TaskSpec(taskType, accession),
                TaskRunMode.valueOf(runMode),
                defaultUser,
                toBoolean(autoDepend));
        return makeMap("id", id);
    }

    private static boolean toBoolean(String stringValue) {
        return "1".equals(stringValue) || "true".equalsIgnoreCase(stringValue) || "yes".equalsIgnoreCase(stringValue);
    }

    private Object processCancel(String taskId) {
        taskManager.cancelTask(Integer.valueOf(taskId), defaultUser);
        return EMPTY;
    }

    private Object processGetStage(String taskType, String accession) {
        TaskStage stage = taskManager.getTaskStage(new TaskSpec(taskType, accession));
        return makeMap("stage", stage.toString());
    }

    private Object processSearchExperiments(String searchText, String fromDate, String toDate, String pendingOnlyStr) {
        searchText = searchText.toLowerCase();
        boolean pendingOnly = toBoolean(pendingOnlyStr);

        // TODO: it's just a murder. grimy bloody murder.
        List<Experiment> experiments = dao.getAllExperiments().subList(0, 10);

        List<Map> result = new ArrayList<Map>();
        for(Experiment experiment : experiments) {
            final TaskStage stage = taskManager.getTaskStage(new TaskSpec(ExperimentTask.TYPE, experiment.getAccession()));
            boolean searchYes = "".equals(searchText)
                    || experiment.getAccession().toLowerCase().contains(searchText)
                    || experiment.getDescription().toLowerCase().contains(searchText);
            boolean pendingYes = !pendingOnly
                    || !TaskStage.DONE.equals(stage);
            if(searchYes && pendingYes)
                result.add(makeMap(
                        "accession", experiment.getAccession(),
                        "stage", stage.toString()
                ));

        }
        return makeMap("experiments", result);
    }

    public Object process(HttpServletRequest request) {
        String op = request.getParameter("op");

        if("pause".equals(op))
            return processPause();

        else if("restart".equals(op))
            return processRestart();

        else if("tasklist".equals(op))
            return processTaskList();

        else if("enqueue".equals(op))
            return processEnqueue(
                    request.getParameter("type"),
                    request.getParameter("accession"),
                    request.getParameter("runMode"),
                    request.getParameter("autoDepends"));

        else if("cancel".equals(op))
            return processCancel(request.getParameter("id"));

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

        return new ErrorResult("Unknown operation specified: " + op);
    }
}

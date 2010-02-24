package uk.ac.ebi.gxa.requesthandlers.tasks;

import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;
import uk.ac.ebi.gxa.tasks.*;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;
import uk.ac.ebi.gxa.utils.JoinIterator;
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
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Task manager AJAX servlet
 * @author pashky
 */
public class TaskManagerRequestHandler extends AbstractRestRequestHandler {
    private static final Map<Object,Object> EMPTY = makeMap();
    private static TaskUser defaultUser = new TaskUser("user");

    private TaskManager taskManager;
    private AtlasDAO dao;

    static void delay() {
        try { Thread.sleep(1000); } catch(Exception e) {/**/}
    }

    static int DONOTHINGNUM() {
        return (int)Math.round(Math.random() * 20) + 5;
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;

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
                            log.info("Heavy analytics calculations " + i + " for " + experimentAccession);
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
                            log.info("Flooding your disk with netcdfs for " + i + " for " + experimentAccession);
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
        return makeMap(
                "isRunning", taskManager.isRunning(),
                "tasks", new JoinIterator<Task,Task,Map>(
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

    private Object processEnqueue(String taskType, String[] accessions, String runMode, String autoDepend) {
        Map<String,Integer> result = new HashMap<String, Integer>();
        boolean wasRunning = taskManager.isRunning();
        if(wasRunning)
            taskManager.pause();
        for(String accession : accessions) {
            int id = taskManager.enqueueTask(new TaskSpec(taskType, accession),
                    TaskRunMode.valueOf(runMode),
                    defaultUser,
                    toBoolean(autoDepend));
            result.put(accession,  id);
        }
        if(wasRunning)
            taskManager.start(); // TODO: should make batch adds here, huh?
        return result;
    }

    private static boolean toBoolean(String stringValue) {
        return "1".equals(stringValue) || "true".equalsIgnoreCase(stringValue) || "yes".equalsIgnoreCase(stringValue);
    }

    private Object processCancel(String[] taskIds) {
        for(String taskId : taskIds)
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
        return makeMap(
                "experiments", result,
                "indexStage", taskManager.getTaskStage(new TaskSpec(IndexTask.TYPE, "")).toString()
                );
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
                    request.getParameterValues("accession"),
                    request.getParameter("runMode"),
                    request.getParameter("autoDepends"));

        else if("cancel".equals(op))
            return processCancel(request.getParameterValues("id"));

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

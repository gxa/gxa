package uk.ac.ebi.gxa.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.web.admin.AnnotationCommand;
import uk.ac.ebi.gxa.annotator.web.admin.AnnotationCommandListener;
import uk.ac.ebi.gxa.annotator.web.admin.UpdateBioEntityAnnotationCommand;
import uk.ac.ebi.gxa.annotator.web.admin.UpdateMappingCommand;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class AnnotationLoaderTask extends AbstractWorkingTask {
    private static Logger log = LoggerFactory.getLogger(AnnotationLoaderTask.class);

    private static final String TYPE_UPDATEANNOTATIONS = "orgupdate";
    private static final String TYPE_UPDATEMAPPINGS = "mappingupdate";

    private AnnotationLoaderTask(TaskManager taskMan, long taskId,
                                 TaskSpec taskSpec, TaskRunMode runMode,
                                 TaskUser user, boolean runningAutoDependencies) {
        super(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        taskMan.addTaskTag(AnnotationLoaderTask.this, TaskTagType.ANNOTATIONS, getTaskSpec().getAccession());
    }

    @Override
    public void start() {
        if (nothingToDo())
            return;

        startTimer();
        taskMan.updateTaskStage(getTaskSpec(), TaskStatus.INCOMPLETE);
        taskMan.writeTaskLog(AnnotationLoaderTask.this, TaskEvent.STARTED, "");

        taskMan.getAnnotationCommandRunner().run(getAnnotationCommand(), getListner());
    }

    private AnnotationCommand getAnnotationCommand() {
        if (TYPE_UPDATEANNOTATIONS.equals(getTaskSpec().getType()))
            return new UpdateBioEntityAnnotationCommand(getTaskSpec().getAccession(), taskMan.getAtlasProperties().getBatchUpdateSize());
        else if (TYPE_UPDATEMAPPINGS.endsWith(getTaskSpec().getType()))
            return new UpdateMappingCommand(getTaskSpec().getAccession(), taskMan.getAtlasProperties().getBatchUpdateSize());
        throw new IllegalStateException();
    }

    @Override
    public void stop() {
        // can't stop this task as there's no stages and no control of annotationLoader running
    }

    @Override
    public boolean isBlockedBy(Task otherTask) {
        return false;
    }

    public static final TaskFactory FACTORY = new TaskFactory() {
        @Override
        public QueuedTask createTask(TaskManager taskMan, long taskId, TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, boolean runningAutoDependencies) {
            return new AnnotationLoaderTask(taskMan, taskId, taskSpec, runMode, user, runningAutoDependencies);
        }

        @Override
        public boolean isFor(TaskSpec taskSpec) {

            return
                    TYPE_UPDATEANNOTATIONS.equals(taskSpec.getType())
                            || TYPE_UPDATEMAPPINGS.equals(taskSpec.getType())

                    ;
        }
    };

    private AnnotationCommandListener getListner() {
        return new AnnotationCommandListener() {
            @Override
            public void commandSuccess(String msg) {
                taskMan.writeTaskLog(AnnotationLoaderTask.this, TaskEvent.FINISHED, msg);
                taskMan.updateTaskStage(getTaskSpec(), TaskStatus.DONE);

                taskMan.notifyTaskFinished(AnnotationLoaderTask.this); // it's waiting for this
            }

            @Override
            public void commandProgress(String progressStatus) {
                currentProgress = progressStatus;
            }

            @Override
            public void commandError(Throwable error) {
                log.error("Task failed because of:", error);

                taskMan.writeTaskLog(AnnotationLoaderTask.this, TaskEvent.FAILED, error.getMessage());
                taskMan.notifyTaskFinished(AnnotationLoaderTask.this); // it's waiting for this
            }
        };
    }
}

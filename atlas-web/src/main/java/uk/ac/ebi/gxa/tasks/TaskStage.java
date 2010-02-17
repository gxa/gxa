package uk.ac.ebi.gxa.tasks;

/**
 * @author pashky
 */
public class TaskStage {
    public static final TaskStage NONE = new TaskStage("NONE");
    public static final TaskStage DONE = new TaskStage("DONE");
    public static TaskStage valueOf(Enum stage) { return new TaskStage(stage.toString()); }
    public static TaskStage valueOf(String stage) { return new TaskStage(stage); }

    private final String stage;

    private TaskStage(String stage) {
        this.stage = stage;
    }

    public String getStage() {
        return stage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskStage that = (TaskStage) o;

        if (stage != null ? !stage.equals(that.stage) : that.stage != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return stage != null ? stage.hashCode() : 0;
    }

    @Override
    public String toString() {
        return stage;
    }
}

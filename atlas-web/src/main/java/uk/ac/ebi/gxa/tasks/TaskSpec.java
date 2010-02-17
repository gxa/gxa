package uk.ac.ebi.gxa.tasks;

/**
 * @author pashky
 */
public class TaskSpec {
    private final String type;
    private final String accession;

    public TaskSpec(String type, String accession) {
        this.type = type;
        this.accession = accession;
    }

    public String getType() {
        return type;
    }

    public String getAccession() {
        return accession;
    }

    @Override
    public String toString() {
        return type + "Task[" + accession + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskSpec taskSpec = (TaskSpec) o;

        if (accession != null ? !accession.equals(taskSpec.accession) : taskSpec.accession != null) return false;
        if (type != null ? !type.equals(taskSpec.type) : taskSpec.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (accession != null ? accession.hashCode() : 0);
        return result;
    }
}

package uk.ac.ebi.gxa.tasks;

/**
 * @author pashky
 */
public class TaskUser {
    private final String userName;

    public TaskUser(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public String toString() {
        return userName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TaskUser taskUser = (TaskUser) o;

        if (userName != null ? !userName.equals(taskUser.userName) : taskUser.userName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return userName != null ? userName.hashCode() : 0;
    }
}

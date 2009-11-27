package uk.ac.ebi.gxa.loader.listener;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An event object that is used for encapsulating callbacks made by an {@link uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener}
 *
 * @see uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener
 * @author Tony Burdett
 * @date 27-Nov-2009
 */
public class AtlasLoaderEvent {
    private long runTime;
    private TimeUnit timeUnit;
    private Status status;
    private List<Throwable> errors;

    /**
     * An AtlasLoaderEvent that represents a completion with a successful outcome
     *
     * @param runTime  the total running time to load the resource
     * @param timeUnit the units used in the running time of this loader
     */
    public AtlasLoaderEvent(long runTime, TimeUnit timeUnit) {
        this.runTime = runTime;
        this.timeUnit = timeUnit;
        this.status = Status.SUCCESS;
    }

    /**
     * An AtlasLoaderEvent that represents a completion following a failure. Clients should supply the list of errors
     * that resulted in the failure.
     *
     * @param runTime  the total running time to load the resource
     * @param timeUnit the units used in the running time of this loader
     * @param errors   the list of errors that occurred, causing the fail
     */
    public AtlasLoaderEvent(long runTime, TimeUnit timeUnit, List<Throwable> errors) {
        this.runTime = runTime;
        this.timeUnit = timeUnit;
        this.status = Status.FAIL;
        this.errors = errors;
    }

    public long getRunTime() {
        return runTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Status getStatus() {
        return status;
    }

    public List<Throwable> getErrors() {
        return errors;
    }

    public enum Status {
        SUCCESS,
        FAIL
    }
}

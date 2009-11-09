package uk.ac.ebi.gxa.netcdf.generator.listener;

import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 02-Oct-2009
 */
public class NetCDFGenerationEvent {
  private long runTime;
  private TimeUnit timeUnit;
  private Status status;
  private List<Throwable> errors;

  /**
   * An IndexBuilderEvent that represents a completion with a successful
   * outcome
   *
   * @param runTime  the total running time to build this index
   * @param timeUnit the units used in the running time of this index
   */
  public NetCDFGenerationEvent(long runTime, TimeUnit timeUnit) {
    this.runTime = runTime;
    this.timeUnit = timeUnit;
    this.status = Status.SUCCESS;
  }

  /**
   * An IndexBuilderEvent that represents a completion following a failure.
   * Clients should supply the error that resulted in the failure.
   *
   * @param runTime  the total running time to build this index
   * @param timeUnit the units used in the running time of this index
   * @param errors   the list of errors that occurred, causing the fail
   */
  public NetCDFGenerationEvent(long runTime, TimeUnit timeUnit,
                           List<Throwable> errors) {
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

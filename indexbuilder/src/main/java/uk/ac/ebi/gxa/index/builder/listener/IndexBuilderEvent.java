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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.index.builder.listener;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An event that is fired when index building completes or thrwos exceptions.
 * You can use these events to track completion of an index builder, or get hold
 * of any errors that may occur during the building process.
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class IndexBuilderEvent {
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
  public IndexBuilderEvent(long runTime, TimeUnit timeUnit) {
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
  public IndexBuilderEvent(long runTime, TimeUnit timeUnit,
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

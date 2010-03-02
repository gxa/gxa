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
    private List<String> accessions;

    private AtlasLoaderEvent() {

    }
    /**
     * An AtlasLoaderEvent that represents a completion with a successful outcome
     *
     * @param runTime  the total running time to load the resource
     * @param timeUnit the units used in the running time of this loader
     * @param accessions successfully created objects' accessions
     * @return constructed event
     */
    public static AtlasLoaderEvent success(long runTime, TimeUnit timeUnit, List<String> accessions) {
        AtlasLoaderEvent event = new AtlasLoaderEvent();
        event.runTime = runTime;
        event.timeUnit = timeUnit;
        event.status = Status.SUCCESS;
        event.accessions = accessions;
        return event;
    }

    /**
     * An AtlasLoaderEvent that represents a completion following a failure. Clients should supply the list of errors
     * that resulted in the failure.
     *
     * @param runTime  the total running time to load the resource
     * @param timeUnit the units used in the running time of this loader
     * @param errors   the list of errors that occurred, causing the fail
     * @return constructed event
     */
    public static AtlasLoaderEvent error(long runTime, TimeUnit timeUnit, List<Throwable> errors) {
        AtlasLoaderEvent event = new AtlasLoaderEvent();
        event.runTime = runTime;
        event.timeUnit = timeUnit;
        event.status = Status.FAIL;
        event.errors = errors;
        return event;
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

    public List<String> getAccessions() {
        return accessions;
    }

    public enum Status {
        SUCCESS,
        FAIL
    }
}

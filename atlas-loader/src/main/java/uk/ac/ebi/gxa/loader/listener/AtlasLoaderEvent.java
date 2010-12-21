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

/**
 * An event object that is used for encapsulating callbacks made by an {@link uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener}
 *
 * @author Tony Burdett
 * @see uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener
 */
public class AtlasLoaderEvent {
    private List<Throwable> errors;
    private List<String> accessions;
    private boolean recomputeStatistics;

    private AtlasLoaderEvent() {

    }

    public static AtlasLoaderEvent success(List<String> accessions, boolean recomputeStatistics) {
        AtlasLoaderEvent event = new AtlasLoaderEvent();
        event.accessions = accessions;
        event.recomputeStatistics = recomputeStatistics;
        return event;
    }

    /**
     * An AtlasLoaderEvent that represents a completion following a failure. Clients should supply the list of errors
     * that resulted in the failure.
     *
     * @param errors the list of errors that occurred, causing the fail
     * @return constructed event
     */
    public static AtlasLoaderEvent error(List<Throwable> errors) {
        AtlasLoaderEvent event = new AtlasLoaderEvent();
        event.errors = errors;
        return event;
    }

    public List<Throwable> getErrors() {
        return errors;
    }

    public List<String> getAccessions() {
        return accessions;
    }

    public boolean isRecomputeStatistics() {
        return recomputeStatistics;
    }
}

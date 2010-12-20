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

package uk.ac.ebi.gxa.index.builder;

import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;

import java.util.List;

/**
 * Interface for building a Gene Expression Atlas index.
 * <p/>
 * By default, all genes and experiments are included, and all experiments (both pending and non-pending) are included.
 * <p/>
 * If you are using an IndexBuilder in a standalone application (not a web application) and you do not want to reuse
 * IndexBuilder for multiple index building calls, you should make sure you register a listener that performs a {@link
 * #shutdown()} upon completion.  This will allow any resources being used by the IndexBuilder implementation to be
 * reclaimed.  Otherwise, an IndexBuilder instance may run indefinitely.
 *
 * @author Tony Burdett
 */
public interface IndexBuilder {
    /**
     * Set collection of index names to build
     * @param includeIndexes collection of names
     */
    void setIncludeIndexes(List<String> includeIndexes);

    /**
     * Shutdown this IndexBuilder, and release any resources used by it
     *
     * @throws IndexBuilderException if shutdown of this index builder failed for any reason
     */
    void shutdown() throws IndexBuilderException;

    /**
     * Build the index and register a listener that provides a callback on completion of the build task.
     * The listener supplied will provide callbacks whenever the indexbuilder has interesting events to report.
     *
     * @param command command to process
     * @param listener a listener that can be used to supply callbacks when building of the index completes, or when any
     *                 errors occur.
     */
    void doCommand(IndexBuilderCommand command, IndexBuilderListener listener);

    /**
     * Register index update handler. Those handlers will be called when index build starts and finishes.
     * @param handler handler to register
     */
    void registerIndexBuildEventHandler(IndexBuilderEventHandler handler);

    /**
     * Unregister index update handler. Handlers will not receive notifications anymore.
     * @param handler handler to unregister
     */
    void unregisterIndexBuildEventHandler(IndexBuilderEventHandler handler);
}

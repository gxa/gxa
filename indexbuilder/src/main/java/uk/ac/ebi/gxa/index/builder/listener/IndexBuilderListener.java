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

import java.util.EventListener;

/**
 * A Listener that can be used to determine when an IndexBuilder has completed
 * it's execution.
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public interface IndexBuilderListener extends EventListener {
    /**
     * Indicates that building or updating of an index completed successfully
     *
     * @param event the event representing this build success event
     */
    void buildSuccess(IndexBuilderEvent event);

    /**
     * Indicates that building or updating of an index exited with an error
     *
     * @param event the event representing this build failure
     */
    void buildError(IndexBuilderEvent event);

    /**
     * Is called by builder to provide some progress status report
     *
     * @param progressStatus a text string representing human-readable status line of current index builder process
     */
    void buildProgress(String progressStatus);

}

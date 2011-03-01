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

package uk.ac.ebi.gxa.analytics.generator.listener;

/**
 * @author todo
 */
public interface AnalyticsGeneratorListener {
    /**
     * Indicates that building or updating of a set of NetCDFs completed
     * successfully
     */
    void buildSuccess();

    /**
     * Indicates that building or updating of a set of Analyticss exited with an
     * error
     *
     * @param event the event representing this build failure
     */
    void buildError(AnalyticsGenerationEvent event);

    /**
     * Is called by builder to provide some progress status report
     *
     * @param progressStatus a text string representing human-readable status line of current generator process
     */
    void buildProgress(String progressStatus);

    /**
     * Is called by builder to provide a warning
     *
     * @param message text string human-readable warning
     */
    public void buildWarning(String message);
}

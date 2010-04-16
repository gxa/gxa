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

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 27-Nov-2009
 */
public interface AtlasLoaderListener {
    /**
     * Indicates that loading of a resource completed successfully
     *
     * @param event the event representing this build success event
     */
    void loadSuccess(AtlasLoaderEvent event);

    /**
     * Indicates that loading of a resource failed
     *
     * @param event the event representing this build failure
     */
    void loadError(AtlasLoaderEvent event);

    /**
     * Updates loader with current progress
     * @param progress progress message
     */
    void loadProgress(String progress);

    /**
     * Updates loader with warning message
     * @param message warning message
     */
    void loadWarning(String message);
}

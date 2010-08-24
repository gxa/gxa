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

package uk.ac.ebi.gxa.loader.steps;

import uk.ac.ebi.gxa.loader.AtlasLoaderException;

/**
 * An interface for an experiment loading step.
 *
 * @author Nikolay Pultsin
 * @date Aug-2010
 */


public interface Step {
    /**
     * Returns a string that will be displayed in a progress indicator
     * during the step execution.
     */
    String displayName();

    /**
     * Just executes the step. Throwing of an AtlasLoaderException means
     * the loading process should be interrupted and all the consequent
     * stps would not be executed.
     */
    void run() throws AtlasLoaderException;
}

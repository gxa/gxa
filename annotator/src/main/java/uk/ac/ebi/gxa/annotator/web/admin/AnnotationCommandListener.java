/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.web.admin;

import java.util.EventListener;

/**
 * User: nsklyar
 * Date: 01/08/2011
 */
public interface AnnotationCommandListener extends EventListener {
    /**
     * Invoked when the command completed successfully
     *
     * @param msg - a success message
     */
    void commandSuccess(String msg);

    /**
     * Invoked during the command run to provide the progress status of the command
     *
     * @param progressStatus a text string representing human-readable status line of the running command
     */
    void commandProgress(String progressStatus);

    /**
     * Invoked when the command error happen
     *
     * @param error - an exception thrown during the command run
     */
    void commandError(Throwable error);
}

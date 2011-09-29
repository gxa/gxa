/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.loader.listner;

import java.util.EventListener;

/**
 * User: nsklyar
 * Date: 01/08/2011
 */
public interface AnnotationLoaderListener extends EventListener {
    /**
     * Indicates that building or updating of an index completed successfully
     *
     * @param msg
     */
    void buildSuccess(String msg);

    /**
     * Is called by builder to provide some progress status report
     *
     * @param progressStatus a text string representing human-readable status line of current index builder process
     */
    void buildProgress(String progressStatus);

    void buildError(Throwable error);
}

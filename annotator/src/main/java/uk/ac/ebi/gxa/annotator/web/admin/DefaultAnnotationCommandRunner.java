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


import uk.ac.ebi.gxa.annotator.loader.AnnotationProcessor;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.util.concurrent.ExecutorService;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class DefaultAnnotationCommandRunner implements AnnotationCommandRunner {

    private final AnnotationProcessor annotationProcessor;
    private final ExecutorService executor;

    public DefaultAnnotationCommandRunner(AnnotationProcessor annotationProcessor, ExecutorService executor) {
        this.annotationProcessor = annotationProcessor;
        this.executor = executor;
    }

    @Override
    public void run(final AnnotationCommand annotationCommand, final AnnotationCommandListener listener) {

        executor.submit(new Runnable() {
            public void run() {
                try {
                    annotationCommand.execute(annotationProcessor, listener);
                } catch (Throwable e) {
                    listener.commandError(e);
                    throw LogUtil.createUnexpected("Unexpected exception", e);
                }
            }
        });
    }
}

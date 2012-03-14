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

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class UpdateMappingCommand implements AnnotationCommand {

    private final String annSrcId;
    private final int batchSize;

    public UpdateMappingCommand(String annSrcId, int batchSize) {
        this.annSrcId = annSrcId;
        this.batchSize = batchSize;
    }

    @Override
    public void execute(AnnotationProcessor annotationProcessor, AnnotationCommandListener listener) {
        annotationProcessor.updateMappings(annSrcId, batchSize, listener);
    }
}
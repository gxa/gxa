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

package uk.ac.ebi.gxa.annotator.loader;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.web.admin.AnnotationCommandListener;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class AnnotationProcessor {

    @Autowired
    private AnnotationSourceDAO annSrcDAO;

    @Autowired
    private AnnotatorFactory annotatorFactory;

    public AnnotationProcessor() {
    }

    public void updateAnnotations(String annSrcId, int batchSize, AnnotationCommandListener listener) {
        createAnnotator(annSrcId, listener).updateAnnotations(batchSize);
    }

    public void updateMappings(String annSrcId, int batchSize, AnnotationCommandListener listener) {
        createAnnotator(annSrcId, listener).updateMappings(batchSize);
    }

    private AnnotationSource fetchAnnSrcById(String id) {
        AnnotationSource annSrc = null;
        if (!isNullOrEmpty(id)) {
            try {
                final long idL = Long.parseLong(id.trim());
                annSrc = annSrcDAO.getById(idL);
            } catch (NumberFormatException e) {
                /* ignore */
            }
        }

        if (annSrc == null) {
            throw new IllegalStateException("Annotation source not found: id = " + id);
        }
        return annSrc;
    }

    private Annotator createAnnotator(String id, AnnotationCommandListener listener) {
        final AnnotationSource annSource = fetchAnnSrcById(id);        
        Annotator ann = AnnotationSourceType.annSrcTypeOf(annSource)
                .createAnnotator(annotatorFactory, annSource);
        ann.setListener(listener);
        return ann;
    }
}

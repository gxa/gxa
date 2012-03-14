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

package uk.ac.ebi.gxa.annotator.annotationsrc;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.validation.AnnotationSourcePropertiesValidator;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.GeneSigAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;

import java.util.Collection;

/**
 * User: nsklyar
 * Date: 23/01/2012
 */
class GeneSigAnnotationSourceManager extends AnnotationSourceManager<GeneSigAnnotationSource> {

    @Autowired
    private GeneSigAnnotationSourceConverter geneSigAnnotationSourceConverter;

    @Autowired
    private AnnotationSourcePropertiesValidator<GeneSigAnnotationSource> geneSigValidator;

    @Override
    protected Collection<GeneSigAnnotationSource> getCurrentAnnSrcs() {
        return annSrcDAO.getAnnotationSourcesOfType(GeneSigAnnotationSource.class);
    }

    @Override
    protected UpdatedAnnotationSource<GeneSigAnnotationSource> createUpdatedAnnotationSource(GeneSigAnnotationSource annSrc) {
        return new UpdatedAnnotationSource<GeneSigAnnotationSource>(annSrc, false);
    }

    @Override
    protected AnnotationSourceConverter<GeneSigAnnotationSource> getConverter() {
        return geneSigAnnotationSourceConverter;
    }

    @Override
    protected Class<GeneSigAnnotationSource> getClazz() {
        return GeneSigAnnotationSource.class;
    }

    @Override
    public void validateProperties(AnnotationSource annSrc, ValidationReportBuilder reportBuilder) {
        if (isForClass(annSrc.getClass())) {
            geneSigValidator.getInvalidPropertyNames((GeneSigAnnotationSource) annSrc, reportBuilder);
        } else {
            throw new IllegalArgumentException("Cannot validate annotation source " + annSrc.getClass() +
                    ". Class casting problem " + GeneSigAnnotationSource.class);
        }
    }

    @Override
    public boolean isForClass(Class<? extends AnnotationSource> annSrcClass) {
        return annSrcClass.equals(GeneSigAnnotationSource.class);
    }
}
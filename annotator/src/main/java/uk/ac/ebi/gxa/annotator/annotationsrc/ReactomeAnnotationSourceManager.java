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
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ReactomeAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.Collections;

import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.SOFTWARE_NAME_PROPNAME;
import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.SOFTWARE_VERSION_PROPNAME;

/**
 * User: nsklyar
 * Date: 24/04/2013
 */
class ReactomeAnnotationSourceManager extends AbstractAnnotationSourceManager<ReactomeAnnotationSource> {

    @Autowired
    private ReactomeAnnotationSourceConverter reactomeAnnotationSourceConverter;


    @Autowired
    private AnnotationSourceInputValidator<ReactomeAnnotationSource> fileBasedInputValidator;

    @Override
    public Collection<Software> getNewVersionSoftware() {
        return Collections.emptySet();
    }

    @Override
    protected UpdatedAnnotationSource<ReactomeAnnotationSource> createUpdatedAnnotationSource(ReactomeAnnotationSource annSrc) {
        return new UpdatedAnnotationSource<ReactomeAnnotationSource>(annSrc, false);
    }

    @Override
    protected AnnotationSourceConverter<ReactomeAnnotationSource> getConverter() {
        return reactomeAnnotationSourceConverter;
    }

    @Override
    public AnnotationSourceInputValidator<ReactomeAnnotationSource> getInputValidator() {
        return fileBasedInputValidator;
    }

    @Override
    protected Class<ReactomeAnnotationSource> getClazz() {
        return ReactomeAnnotationSource.class;
    }

    @Override
    public void validateProperties(AnnotationSource annSrc, ValidationReportBuilder reportBuilder) {

    }

    @Override
    protected ReactomeAnnotationSource fetchAnnSrcByProperties(String text) {
        AnnotationSourceProperties properties = AnnotationSourceProperties.createPropertiesFromText(text);
        return annSrcDAO.findFileBasedAnnotationSource(properties.getProperty(SOFTWARE_NAME_PROPNAME),
                properties.getProperty(SOFTWARE_VERSION_PROPNAME), ReactomeAnnotationSource.class);
    }

    @Override
    protected Class<ReactomeAnnotationSource> getAnnSrcClass() {
        return ReactomeAnnotationSource.class;
    }

    @Override
    public boolean isForClass(Class<? extends AnnotationSource> annSrcClass) {
        return annSrcClass.equals(ReactomeAnnotationSource.class);
    }
}

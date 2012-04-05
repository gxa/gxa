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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * User: nsklyar
 * Date: 26/10/2011
 */
@Service
abstract class AbstractAnnotationSourceManager<T extends AnnotationSource>{

    @Autowired
    protected AnnotationSourceDAO annSrcDAO;

    @Autowired
    protected SoftwareDAO softwareDAO;

    public String getAnnSrcString(long id) throws RecordNotFoundException {
        return getAnnSrcString(getAnnSrc(id));
    }

    public String getAnnSrcString(T annSrc) throws RecordNotFoundException {
        return getConverter().convertToString(annSrc);
    }

    public T getAnnSrc(long id) throws RecordNotFoundException {
        T annSource = fetchAnnSrcById(id);
        if (annSource == null) {
            throw new RecordNotFoundException("AnnotationSource not found: id=" + id);
        }
        return annSource;
    }

    @Transactional
    public ValidationReportBuilder validateAndSaveAnnSrc(long id, String text) {
        final ValidationReportBuilder errors = new ValidationReportBuilder();
        T annSrc = fetchAnnSrcById(id);
        return validateAndSaveAnnSrc(text, annSrc, errors);
    }

    @Transactional
    public ValidationReportBuilder updateLatestAnnotationSources(String text, String separator, ValidationReportBuilder errors ){
        final Collection<String> stringSourcesOfType = AnnotationSourcesExporter.getStringSourcesOfType(text, getAnnSrcClass().getSimpleName(), separator);
        for (String stringSource : stringSourcesOfType) {
            T annSrc = fetchAnnSrcByProperties(stringSource);
            validateAndSaveAnnSrc(stringSource, annSrc, errors);
        }
        return errors;
    }

    public String getLatestAnnotationSourcesAsText(String separator) {

        final Collection<T> sources = annSrcDAO.getLatestAnnotationSourcesOfType(getAnnSrcClass());
        Collection<String> sourceStrings = Collections2.transform(sources, new Function<T, String>() {
            @Override
            public String apply(@Nullable T source) {
                return getConverter().convertToString(source);
            }
        });

        return AnnotationSourcesExporter.joinAsText(sourceStrings, getAnnSrcClass().getSimpleName(), separator);
    }

    private ValidationReportBuilder validateAndSaveAnnSrc(String text, T annSrc, ValidationReportBuilder errors) {
        try {
            //validate input form
            if (!getInputValidator().isValidInputText(text, errors)) return errors;

            if (annSrc == null) {
                annSrc = createNewAnnotationSource(text, errors);
            } else {
                editExistingAnnotationSource(annSrc, text, errors);
            }

            if (!errors.isEmpty()) return errors;

            if (annSrc.isObsolete()) {
                errors.addMessage("Can't save. The annotation source is obsolete.");
            } else {
                annSrcDAO.save(annSrc);
            }

            return errors;
        } catch (AnnotationLoaderException e) {
            throw LogUtil.createUnexpected("Cannot save Annotation Source: " + e.getMessage(), e);
        }
    }

    private void editExistingAnnotationSource(T annSrc, String text, ValidationReportBuilder errors) throws AnnotationLoaderException {
        if (getInputValidator().isImmutableFieldsValid(annSrc, text, errors)) {
            getConverter().editAnnotationSource(annSrc, text);
        }
    }

    private T createNewAnnotationSource(String text, ValidationReportBuilder errors) throws AnnotationLoaderException {
        final AnnotationSourceInputValidator<T> inputValidator = getInputValidator();

        if (!inputValidator.isNewAnnSrcUnique(text, errors)) return null;

        T annSrc = getConverter().initAnnotationSource(text);
        getConverter().editAnnotationSource(annSrc, text);
        return annSrc;
    }


    public abstract void validateProperties(AnnotationSource annSrc, ValidationReportBuilder reportBuilder);

    protected abstract T fetchAnnSrcByProperties(String text);

    protected abstract Class<T> getAnnSrcClass();

    public abstract boolean isForClass(Class<? extends AnnotationSource> annSrcClass);

    @Transactional
    public abstract Collection<Software> getNewVersionSoftware();

    protected abstract UpdatedAnnotationSource<T> createUpdatedAnnotationSource(T annSrc);

    protected abstract AnnotationSourceConverter<T> getConverter();

    public abstract AnnotationSourceInputValidator<T> getInputValidator();

    protected T fetchAnnSrcById(long id) {
        return annSrcDAO.getById(id, getClazz());
    }

    protected abstract Class<T> getClazz();


    public void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

    public void setSoftwareDAO(SoftwareDAO softwareDAO) {
        this.softwareDAO = softwareDAO;
    }

}

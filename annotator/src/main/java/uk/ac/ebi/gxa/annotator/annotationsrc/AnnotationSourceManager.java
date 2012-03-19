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

import org.apache.commons.lang.StringUtils;
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

import javax.persistence.EntityNotFoundException;
import java.util.Collection;
import java.util.HashSet;

/**
 * User: nsklyar
 * Date: 26/10/2011
 */
@Service
abstract class AnnotationSourceManager<T extends AnnotationSource> {

    @Autowired
    protected AnnotationSourceDAO annSrcDAO;

    @Autowired
    protected SoftwareDAO softwareDAO;

    @Transactional
    public Collection<UpdatedAnnotationSource<T>> getCurrentAnnotationSources() {
        final Collection<UpdatedAnnotationSource<T>> result = new HashSet<UpdatedAnnotationSource<T>>();
        final Collection<T> currentAnnSrcs = getCurrentAnnSrcs();
        for (T currentAnnSrc : currentAnnSrcs) {
            result.add(createUpdatedAnnotationSource(currentAnnSrc));
        }
        return result;
    }

    public String getAnnSrcString(long id) throws RecordNotFoundException {
        return getConverter().convertToString(getAnnSrc(id));
    }

    public T getAnnSrc(long id) throws RecordNotFoundException {
        T annSource = fetchAnnSrcById(id);
        if (annSource == null) {
            throw new RecordNotFoundException("AnnotationSource not found: id=" + id);
        }
        return annSource;
    }

    @Transactional
    public Collection<String> validateAndSaveAnnSrc(long id, String text) {
        final AnnotationSourceConverter<T> converter = getConverter();
        try {
            final T annSrc = fetchAnnSrcById(id);
            final ValidationReportBuilder reportBuilder = new ValidationReportBuilder();
            final AnnotationSource annotationSource = converter.editOrCreateAnnotationSource(annSrc, text, reportBuilder);
            if (reportBuilder.isEmpty()) {
                annSrcDAO.save(annotationSource);

            }
            return reportBuilder.getMessages();
        } catch (AnnotationLoaderException e) {
            throw LogUtil.createUnexpected("Cannot save Annotation Source: " + e.getMessage(), e);
        }
    }

    public abstract void validateProperties(AnnotationSource annSrc, ValidationReportBuilder reportBuilder);

    public void validateProperties(long annSrcId, ValidationReportBuilder reportBuilder) {
        validateProperties(fetchAnnSrcById(annSrcId), reportBuilder);
    }


    public abstract boolean isForClass(Class<? extends AnnotationSource> annSrcClass);

    protected abstract Collection<T> getCurrentAnnSrcs();

    @Transactional
    public abstract Collection<Software> getNewVersionSoftware();

    protected abstract UpdatedAnnotationSource<T> createUpdatedAnnotationSource(T annSrc);

    protected abstract AnnotationSourceConverter<T> getConverter();

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

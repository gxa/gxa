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
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.annotation.Nullable;
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

    public String getAnnSrcString(String id) {
        return getConverter().convertToString(fetchAnnSrcById(id));
    }

    @Transactional
    public ValidationReportBuilder saveAnnSrc(String id, String text) {
        final AnnotationSourceConverter<T> converter = getConverter();
        try {
            final T annSrc = fetchAnnSrcById(id);
            final ValidationReportBuilder reportBuilder = new ValidationReportBuilder();
            final AnnotationSource annotationSource = converter.editOrCreateAnnotationSource(annSrc, text, reportBuilder);
            if (reportBuilder.isEmpty()) {
                annSrcDAO.save(annotationSource);
            }
            return reportBuilder;
        } catch (AnnotationLoaderException e) {
            throw LogUtil.createUnexpected("Cannot save Annotation Source: " + e.getMessage(), e);
        }
    }

    public abstract void validateProperties(AnnotationSource annSrc, ValidationReportBuilder reportBuilder);

    public void validateProperties(String annSrcId, ValidationReportBuilder reportBuilder) {
        validateProperties(fetchAnnSrcById(annSrcId), reportBuilder);
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

    public void updateLatestAnnotation(String text, String separator) throws AnnotationLoaderException {
        final Collection<String> stringSourcesOfType = AnnotationSourcesExporter.getStringSourcesOfType(text, getAnnSrcClass().getSimpleName(), separator);
        for (String stringSource : stringSourcesOfType) {
            final ValidationReportBuilder reportBuilder = new ValidationReportBuilder();
            getConverter().editOrCreateAnnotationSource(null, stringSource, reportBuilder);
        }

    }

    protected abstract Class<T> getAnnSrcClass();

    public abstract boolean isForClass(Class<? extends AnnotationSource> annSrcClass);

    @Transactional
    public abstract Collection<Software> getNewVersionSoftware();

    protected abstract UpdatedAnnotationSource<T> createUpdatedAnnotationSource(T annSrc);

    protected abstract AnnotationSourceConverter<T> getConverter();

    protected T fetchAnnSrcById(String id) {
        T annSrc = null;
        if (!StringUtils.isEmpty(id)) {
            try {
                final long idL = Long.parseLong(id.trim());
                annSrc = annSrcDAO.getById(idL, getClazz());
            } catch (NumberFormatException e) {
                throw LogUtil.createUnexpected("Cannot fetch Annotation Source. Wrong ID ", e);
            }
        }
        return annSrc;
    }

    private Collection<T> getCurrentAnnSrcs() {
        return annSrcDAO.getLatestAnnotationSourcesOfType(getAnnSrcClass());
    }

    protected abstract Class<T> getClazz();


    public void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

    public void setSoftwareDAO(SoftwareDAO softwareDAO) {
        this.softwareDAO = softwareDAO;
    }

}

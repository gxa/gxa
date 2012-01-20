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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.connection.AnnotationSourceAccessException;
import uk.ac.ebi.gxa.annotator.model.connection.AnnotationSourceConnection;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.HashSet;

/**
 * User: nsklyar
 * Date: 26/10/2011
 */
@Service
public class AnnotationSourceManager {

    @Autowired
    protected AnnotationSourceDAO annSrcDAO;
    @Autowired
    protected SoftwareDAO softwareDAO;

    @Autowired
    protected ConverterFactory annotationSourceConverterFactory;

    @Transactional
    public <T extends AnnotationSource> Collection<AnnotationSource> getCurrentAnnotationSourcesOfType(Class<T> type) {
        final Collection<AnnotationSource> result = new HashSet<AnnotationSource>();
        final Collection<T> currentAnnSrcs = annSrcDAO.getAnnotationSourcesOfType(type);
        for (AnnotationSource annSrc : currentAnnSrcs) {
            try {
                AnnotationSourceConnection connection = annSrc.createConnection();
                String newVersion = connection.getOnlineSoftwareVersion();

                if (annSrc.getSoftware().getVersion().equals(newVersion)) {
                    result.add(annSrc);
                } else {
                    //check if AnnotationSource exists for new version
                    Software newSoftware = softwareDAO.findOrCreate(annSrc.getSoftware().getName(), newVersion);
                    AnnotationSource newAnnSrc = annSrc.createCopyForNewSoftware(newSoftware);
                    annSrcDAO.save(newAnnSrc);
                    result.add(newAnnSrc);
                    annSrcDAO.remove(annSrc);
                }
            } catch (AnnotationSourceAccessException e) {
                throw LogUtil.createUnexpected("Problem when fetching version for " + annSrc.getSoftware().getName(), e);
            }
        }
        return result;
    }

    public String getAnnSrcString(String id, AnnotationSourceType type) {
        final AnnotationSourceConverter converter = type.createConverter(annotationSourceConverterFactory);
        return converter.convertToString(id);
    }

    @Transactional
    public void saveAnnSrc(String id, AnnotationSourceType type, String text) {
        final AnnotationSourceConverter converter = type.createConverter(annotationSourceConverterFactory);
        try {
            final AnnotationSource annotationSource = converter.editOrCreateAnnotationSource(id, text);
            annSrcDAO.save(annotationSource);
        } catch (AnnotationLoaderException e) {
            throw LogUtil.createUnexpected("Cannot save Annotation Source: " + e.getMessage(), e);
        }
    }

    public void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

    public void setSoftwareDAO(SoftwareDAO softwareDAO) {
        this.softwareDAO = softwareDAO;
    }

    public void setAnnotationSourceConverterFactory(ConverterFactory annotationSourceConverterFactory) {
        this.annotationSourceConverterFactory = annotationSourceConverterFactory;
    }
}

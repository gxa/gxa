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
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * User: nsklyar
 * Date: 23/01/2012
 */
public class TopAnnotationSourceManager {

    private List<AnnotationSourceManager<? extends AnnotationSource>> managers;

    @Autowired
    private AnnotationSourceDAO annSrcDAO;

    @Autowired
    private SoftwareDAO softwareDAO;

    public TopAnnotationSourceManager(List<AnnotationSourceManager<? extends AnnotationSource>> managers) {
        this.managers = managers;
    }

    public Collection<UpdatedAnnotationSource> getAllAnnotationSources() {
        Collection<UpdatedAnnotationSource> result = new HashSet<UpdatedAnnotationSource>();
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            result.addAll(manager.getCurrentAnnotationSources());
        }

        return result;
    }

    public Software getSoftware(long softwareId) throws RecordNotFoundException {
        Software software = annSrcDAO.getSoftwareById(softwareId);
        if (software == null) {
            throw new RecordNotFoundException("Software not found: id = " + softwareId);
        }
        return software;
    }

    public Collection<AnnotationSource> getAnnotationSourcesBySoftware(Software software) {
        return annSrcDAO.getAnnotationSourceForSoftware(software);
    }

    public List<Software> getAllSoftware() {
        List<Software> result = new ArrayList<Software>();
        final List<Software> softwares = softwareDAO.getAllButLegacySoftware();
        result.addAll(softwares);
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            result.addAll(manager.getNewVersionSoftware());
        }
        return result;
    }
    
    public void validateProperties(AnnotationSource annSrc, ValidationReportBuilder reportBuilder) {
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            if (manager.isForClass(annSrc.getClass())) {
                manager.validateProperties(annSrc, reportBuilder);
                return;
            }
        }
        throw new IllegalArgumentException("Cannot validate annotation source of class " + annSrc.getClass().getName());

    }

    public String getAnnSrcString(long id, AnnotationSourceType type) throws RecordNotFoundException {
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            if (manager.isForClass(type.getClazz())) {
                return manager.getAnnSrcString(id);
            }
        }

        throw new IllegalArgumentException("Annotation source manager is not available for type " + type);
    }

    public ValidationReportBuilder saveAnnSrc(long id, String text, AnnotationSourceType type) {
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            if (manager.isForClass(type.getClazz())) {
                return manager.saveAnnSrc(id, text);
            }
        }
        throw new IllegalArgumentException("Annotation source manager is not available for type " + type);
    }

    public void validateProperties(long annSrcId, AnnotationSourceType type, ValidationReportBuilder reportBuilder) {
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            if (manager.isForClass(type.getClazz())) {
                manager.validateProperties(annSrcId, reportBuilder);
                return;
            }
        }
        throw new IllegalArgumentException("Cannot validate annotation source of class " + type);

    }
}

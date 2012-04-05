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
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.List;

/**
 * User: nsklyar
 * Date: 03/04/2012
 */
@Service
class SoftwareManager {

    @Autowired
    private AnnotationSourceDAO annSrcDAO;

    @Autowired
    private SoftwareDAO softwareDAO;

    public Collection<AnnotationSource> getAnnotationSourcesBySoftware(Software software) {
        return annSrcDAO.getAnnotationSourceForSoftware(software);
    }

    public List<Software> getAllSoftware() {
        return softwareDAO.getAllButLegacySoftware();
    }

    public Software getSoftware(long softwareId) throws RecordNotFoundException {
        Software software = softwareDAO.getById(softwareId);
        if (software == null) {
            throw new RecordNotFoundException("Software not found: id = " + softwareId);
        }
        return software;
    }

    @Transactional
    public Software activateSoftware(long softwareId) throws RecordNotFoundException {
        Software software = softwareDAO.getById(softwareId);
        if (software == null) {
            throw new RecordNotFoundException("Software not found: id = " + softwareId);
        }

        final List<Software> activeSoftwares = softwareDAO.getActiveSoftwares();
        for (Software activeSoftware : activeSoftwares) {
            if (software.getName().equalsIgnoreCase(activeSoftware.getName())) {
                activeSoftware.setActive(false);
                softwareDAO.save(activeSoftware);
            }
        }
        software.setActive(true);
        softwareDAO.save(software);

        return software;
    }

    protected void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

    protected void setSoftwareDAO(SoftwareDAO softwareDAO) {
        this.softwareDAO = softwareDAO;
    }
}

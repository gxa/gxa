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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementMappingData;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.web.admin.AnnotationCommandListener;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * User: nsklyar
 * Date: 27/06/2011
 */
@Service
public class AtlasBioEntityDataWriter {

    private static Logger log = LoggerFactory.getLogger(AtlasBioEntityDataWriter.class);

    @Autowired
    private BioEntityDAO bioEntityDAO;

    @Autowired
    private AnnotationSourceDAO annSrcDAO;

    public AtlasBioEntityDataWriter() {
    }

    @Transactional
    public void writeBioEntities(final BioEntityData data, AnnotationCommandListener listener) {
        for (BioEntityType type : data.getBioEntityTypes()) {
            reportProgress("Writing bioentities of type " + type.getName() + " for Organism " + getOrganismNames(data).toString(), listener);
            Collection<BioEntity> bioEntities = data.getBioEntitiesOfType(type);
            bioEntityDAO.writeBioEntities(bioEntities);
        }
    }

    @Transactional
    public void writePropertyValues(final Collection<BEPropertyValue> propertyValues, AnnotationCommandListener listener) {
        reportProgress("Writing " + propertyValues.size() + " property values", listener);
        bioEntityDAO.writePropertyValues(propertyValues);
    }

    @Transactional
    public void writeBioEntityToPropertyValues(final BioEntityAnnotationData data,
                                               final AnnotationSource annSrc,
                                               boolean checkBioEntities,
                                               AnnotationCommandListener listener) {
        if (annSrc.isApplied()) {
            if (data.getOrganisms().isEmpty()) {
                deleteBioEntityToPropertyValues(annSrc.getSoftware(), listener);
            }
            for (Organism organism : data.getOrganisms()) {
                deleteBioEntityToPropertyValues(organism, annSrc.getSoftware(), listener);
            }
        }
        for (BioEntityType type : data.getBioEntityTypes()) {
            Collection<Pair<String, BEPropertyValue>> propValues = data.getPropertyValuesForBioEntityType(type);
            reportProgress("Writing " + propValues.size() + " property values for " + type.getName() + " Organism: " + getOrganismNames(data), listener);
            if (checkBioEntities) {
                bioEntityDAO.writeBioEntityToPropertyValuesChecked(propValues, type, annSrc.getSoftware());
            } else {
                bioEntityDAO.writeBioEntityToPropertyValues(propValues, type, annSrc.getSoftware());
            }
        }

        annSrc.setApplied(true);
        annSrcDAO.update(annSrc);
    }

    private void deleteBioEntityToPropertyValues(final Organism organism, final Software software, AnnotationCommandListener listener) {
        reportProgress("Annotations for organism " + organism.getName() +
                " already loaded and are going to be deleted before reloading ", listener);
        int count = bioEntityDAO.deleteBioEntityToPropertyValues(organism, software);
        reportProgress("Deleted " + count + " annotations.", listener);
    }

    private void deleteBioEntityToPropertyValues(final Software software, AnnotationCommandListener listener) {
        reportProgress("Annotations for software " + software.getFullName() +
                " already loaded and are going to be deleted before reloading ", listener);
        int count = bioEntityDAO.deleteBioEntityToPropertyValues(software);
        reportProgress("Deleted " + count + " annotations.", listener);
    }


    @Transactional
    public void writeDesignElements(final DesignElementMappingData data, final ArrayDesign arrayDesign, final Software software, boolean deleteBeforeWrite, AnnotationCommandListener listener) {
        if (deleteBeforeWrite) {
            deleteDesignElementBioEntityMappings(software, arrayDesign, listener);
        }
        reportProgress("Writing " + data.getDesignElements().size() + " design elements of " + arrayDesign.getAccession(), listener);
        bioEntityDAO.writeDesignElements(data.getDesignElements(), arrayDesign);
        for (BioEntityType bioEntityType : data.getBioEntityTypes()) {
            Collection<Pair<String, String>> designElementToBioEntity = data.getDesignElementToBioEntity(bioEntityType);
            reportProgress("Writing " + designElementToBioEntity.size() + " design elements to " +
                    bioEntityType.getName() + " mappings of " + arrayDesign.getAccession(), listener);
            bioEntityDAO.writeDesignElementBioEntityMappings(designElementToBioEntity,
                    bioEntityType,
                    software,
                    arrayDesign);
        }
    }

    private void deleteDesignElementBioEntityMappings(final Software software, final ArrayDesign arrayDesign, AnnotationCommandListener listener) {
        reportProgress("Mappings for array design " + arrayDesign.getAccession() +
                " already loaded and are going to be deleted before reloading ", listener);
        int count = bioEntityDAO.deleteDesignElementBioEntityMappings(software, arrayDesign);
        reportProgress("Deleted " + count + " mappings.", listener);
    }

    private Collection<String> getOrganismNames(final BioEntityData data) {
        return Collections2.transform(data.getOrganisms(), new Function<Organism, String>() {
            @Override
            public String apply(@Nonnull Organism organism) {
                return organism.getName();
            }
        });
    }

    void reportProgress(String report, AnnotationCommandListener listener) {
        log.info(report);
        if (listener != null)
            listener.buildProgress(report);
    }
}

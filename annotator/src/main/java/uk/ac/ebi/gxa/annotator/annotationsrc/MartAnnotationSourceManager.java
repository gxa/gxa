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
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.AnnotationSourcePropertiesValidator;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.annotator.validation.VersionFinder;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.*;

/**
 * User: nsklyar
 * Date: 23/01/2012
 */
class MartAnnotationSourceManager extends AbstractAnnotationSourceManager<BioMartAnnotationSource> {

    @Autowired
    private BioMartAnnotationSourceConverter bioMartAnnotationSourceConverter;

    @Autowired
    private AnnotationSourcePropertiesValidator<BioMartAnnotationSource> martValidator;

    @Autowired
    private VersionFinder<BioMartAnnotationSource> martVersionFinder;

    @Autowired
    private AnnotationSourceInputValidator<BioMartAnnotationSource> bioMartInputValidator;

    @Override
    @Transactional(rollbackFor = Exception.class) //ToDo: roll back doesn't work, investigate more
    public Collection<Software> getNewVersionSoftware() {
        Set<Software> newSoftwares = new HashSet<Software>();
        final Collection<BioMartAnnotationSource> currentAnnSrcs = annSrcDAO.getLatestAnnotationSourcesOfType(BioMartAnnotationSource.class);
        for (BioMartAnnotationSource annSrc : currentAnnSrcs) {
            String newVersion = martVersionFinder.fetchOnLineVersion(annSrc);
            if (!annSrc.getSoftware().getVersion().equals(newVersion)) {
                Software newSoftware = softwareDAO.findOrCreate(annSrc.getSoftware().getName(), newVersion);
                newSoftwares.add(newSoftware);

                BioMartAnnotationSource newAnnSrc = annSrc.createCopyForNewSoftware(newSoftware);
                annSrcDAO.save(newAnnSrc);
                annSrc.setObsolete(true);
                annSrcDAO.update(annSrc);
            }
        }
        return newSoftwares;
    }

    /**
     * @deprecated
     */
    @Override
    @Transactional
    protected UpdatedAnnotationSource<BioMartAnnotationSource> createUpdatedAnnotationSource(BioMartAnnotationSource annSrc) {
        final String newVersion = martVersionFinder.fetchOnLineVersion(annSrc);
        if (annSrc.getSoftware().getVersion().equals(newVersion)) {
            return new UpdatedAnnotationSource<BioMartAnnotationSource>(annSrc, false);
        } else {
            Software newSoftware = softwareDAO.findOrCreate(annSrc.getSoftware().getName(), newVersion);
            BioMartAnnotationSource newAnnSrc = annSrc.createCopyForNewSoftware(newSoftware);
            annSrcDAO.remove(annSrc);
            annSrcDAO.save(newAnnSrc);
            return new UpdatedAnnotationSource<BioMartAnnotationSource>(newAnnSrc, true);
        }
    }

    @Override
    protected BioMartAnnotationSourceConverter getConverter() {
        return bioMartAnnotationSourceConverter;
    }

    public AnnotationSourceInputValidator<BioMartAnnotationSource> getInputValidator() {
        return bioMartInputValidator;
    }

    @Override
    protected Class<BioMartAnnotationSource> getClazz() {
        return BioMartAnnotationSource.class;
    }

    @Override
    public void validateProperties(AnnotationSource annSrc, ValidationReportBuilder reportBuilder) {
        if (isForClass(annSrc.getClass())) {
            martValidator.validatePropertyNames((BioMartAnnotationSource) annSrc, reportBuilder);
        } else {
            throw new IllegalArgumentException("Cannot validate annotation source " + annSrc.getClass() +
                    ". Class casting problem " + BioMartAnnotationSource.class);
        }
    }

    @Override
    protected BioMartAnnotationSource fetchAnnSrcByProperties(String text) {
        AnnotationSourceProperties properties = AnnotationSourceProperties.createPropertiesFromText(text);
        return annSrcDAO.findBioMartAnnotationSource(properties.getProperty(SOFTWARE_NAME_PROPNAME),
                properties.getProperty(SOFTWARE_VERSION_PROPNAME),
                properties.getProperty(ORGANISM_PROPNAME));
    }

    @Override
    protected Class<BioMartAnnotationSource> getAnnSrcClass() {
        return BioMartAnnotationSource.class;
    }

    @Override
    public boolean isForClass(Class<? extends AnnotationSource> annSrcClass) {
        return annSrcClass.equals(BioMartAnnotationSource.class);
    }

    protected void setMartVersionFinder(VersionFinder<BioMartAnnotationSource> martVersionFinder) {
        this.martVersionFinder = martVersionFinder;
    }

    protected void setConverter(BioMartAnnotationSourceConverter bioMartAnnotationSourceConverter) {
        this.bioMartAnnotationSourceConverter = bioMartAnnotationSourceConverter;
    }
}

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
import uk.ac.ebi.gxa.annotator.loader.biomart.MartPropertiesValidator;
import uk.ac.ebi.gxa.annotator.loader.biomart.MartVersionFinder;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;

/**
 * User: nsklyar
 * Date: 23/01/2012
 */
 class MartAnnotationSourceManager extends AnnotationSourceManager<BioMartAnnotationSource> {

    @Autowired
    private BioMartAnnotationSourceConverter bioMartAnnotationSourceConverter;

    @Autowired
    private MartPropertiesValidator validator;
    
    @Autowired
    private MartVersionFinder versionFinder;

    @Override
    protected Collection<BioMartAnnotationSource> getCurrentAnnSrcs() {
        return annSrcDAO.getAnnotationSourcesOfType(BioMartAnnotationSource.class);
    }

    @Override
    protected UpdatedAnnotationSource<BioMartAnnotationSource> createUpdatedAnnotationSource(BioMartAnnotationSource annSrc) {
        final String newVersion = versionFinder.fetchOnLineVersion(annSrc);
        if (annSrc.getSoftware().getVersion().equals(newVersion)) {
            return new UpdatedAnnotationSource<BioMartAnnotationSource>(annSrc, false);
        } else {
            Software newSoftware = softwareDAO.findOrCreate(annSrc.getSoftware().getName(), newVersion);
            BioMartAnnotationSource newAnnSrc = annSrc.createCopyForNewSoftware(newSoftware);
            annSrcDAO.save(newAnnSrc);
            annSrcDAO.remove(annSrc);
            return new UpdatedAnnotationSource<BioMartAnnotationSource>(newAnnSrc, true);
        }
    }

    @Override
    protected AnnotationSourceConverter getConverter() {
        return bioMartAnnotationSourceConverter;
    }

    @Override
    public Collection<String> validateProperties(AnnotationSource annSrc) {
        if (isForClass(annSrc.getClass())) {
            return validator.getInvalidPropertyNames((BioMartAnnotationSource) annSrc);
        }
        throw new IllegalArgumentException("Cannot validate annotation source " + annSrc.getClass() +
                ". Class casting problem "  + BioMartAnnotationSource.class);
    }

    @Override
    public String validateStructure(BioMartAnnotationSource annSrc) {
        return bioMartAnnotationSourceConverter.validateStructure(annSrc);
    }

    @Override
    public boolean isForClass(Class<? extends AnnotationSource> annSrcClass) {
        return annSrcClass.equals(BioMartAnnotationSource.class);
    }

}

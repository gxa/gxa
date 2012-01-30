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

import org.apache.commons.configuration.PropertiesConfiguration;
import uk.ac.ebi.gxa.annotator.model.GeneSigAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Properties;

/**
 * User: nsklyar
 * Date: 10/11/2011
 */
class GeneSigAnnotationSourceConverter extends AnnotationSourceConverter<GeneSigAnnotationSource> {

    @Override
    protected Class<GeneSigAnnotationSource> getClazz() {
        return GeneSigAnnotationSource.class;
    }
    
    @Override
    protected GeneSigAnnotationSource initAnnotationSource(GeneSigAnnotationSource annSrc, Properties properties) throws AnnotationLoaderException {
        Software software = softwareDAO.findOrCreate(getProperty(SOFTWARE_NAME_PROPNAME, properties), getProperty(SOFTWARE_VERSION_PROPNAME, properties));

        if (annSrc == null) {
            return new GeneSigAnnotationSource(software);
        }

        if (!annSrc.getSoftware().equals(software)) {
            throw new AnnotationLoaderException("Software should not be changed when editing Annotation Source!");
        }
        return annSrc;
    }

    @Override
    protected void updateExtraProperties(Properties properties, GeneSigAnnotationSource annotationSource) throws AnnotationLoaderException {
    }

    @Override
    protected void writeExtraProperties(GeneSigAnnotationSource annSrc, PropertiesConfiguration properties) {
    }


}

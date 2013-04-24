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

import uk.ac.ebi.gxa.annotator.model.ReactomeAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.SOFTWARE_NAME_PROPNAME;
import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.SOFTWARE_VERSION_PROPNAME;

/**
 * User: nsklyar
 * Date: 10/11/2011
 */
class ReactomeAnnotationSourceConverter extends AnnotationSourceConverter<ReactomeAnnotationSource> {


    @Override
    protected ReactomeAnnotationSource initAnnotationSource(String text) {
        AnnotationSourceProperties properties = AnnotationSourceProperties.createPropertiesFromText(text);

        Software software = softwareDAO.findOrCreate(properties.getProperty(SOFTWARE_NAME_PROPNAME), properties.getProperty(SOFTWARE_VERSION_PROPNAME));
        return new ReactomeAnnotationSource(software);
    }

    @Override
    protected void updateExtraProperties(AnnotationSourceProperties properties, ReactomeAnnotationSource annotationSource) throws AnnotationLoaderException {
    }

    @Override
    protected void writeExtraProperties(ReactomeAnnotationSource annSrc, AnnotationSourceProperties properties) {
    }


}

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

import uk.ac.ebi.gxa.annotator.model.GeneSigAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.Collection;
import java.util.Collections;

import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.*;

/**
 * User: nsklyar
 * Date: 21/03/2012
 */
public class GeneSigInputValidator extends AnnotationSourceInputValidator<GeneSigAnnotationSource> {


    @Override
    protected boolean isImmutableFieldsValid(GeneSigAnnotationSource annSrc, String text, ValidationReportBuilder reportBuilder) {
        AnnotationSourceProperties properties = AnnotationSourceProperties.createPropertiesFromText(text);
        Software software = softwareDAO.findOrCreate(properties.getProperty(SOFTWARE_NAME_PROPNAME), properties.getProperty(SOFTWARE_VERSION_PROPNAME));
        if (!annSrc.getSoftware().equals(software)) {
            reportBuilder.addMessage("Software should not be changed when editing Annotation Source!");
            return false;
        }
        return true;
    }

    @Override
    protected Collection<String> getRequiredProperties() {
        return Collections.unmodifiableCollection(PROPNAMES);
    }

    @Override
    public boolean isNewAnnSrcUnique(String text, ValidationReportBuilder reportBuilder) {
        AnnotationSourceProperties properties = new AnnotationSourceProperties();
        Software software = new Software(properties.getProperty(SOFTWARE_NAME_PROPNAME), properties.getProperty(SOFTWARE_VERSION_PROPNAME));
        if (annSrcDAO.findGeneSigAnnotationSource(software.getName(), software.getVersion()) != null) {
            reportBuilder.addMessage("Annotation source with software " + software.getName() + "/" +
                    software.getVersion() + " already exists. If you need to " +
                    "change it use Edit button");
            return false;
        }
        return true;
    }

    @Override
    protected void extraValidation(AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) {
    }

}

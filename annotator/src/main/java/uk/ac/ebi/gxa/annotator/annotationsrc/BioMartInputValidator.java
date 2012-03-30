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
import uk.ac.ebi.gxa.annotator.loader.biomart.MartVersionFinder;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.*;

/**
 * User: nsklyar
 * Date: 21/03/2012
 */
class BioMartInputValidator extends AnnotationSourceInputValidator<BioMartAnnotationSource> {

    @Autowired
    MartVersionFinder martVersionFinder;

    @Override
    protected boolean isImmutableFieldsValid(BioMartAnnotationSource annSrc, String text, ValidationReportBuilder reportBuilder) {
        final AnnotationSourceProperties properties = AnnotationSourceProperties.createPropertiesFromText(text);
        boolean isValid = true;
        if (!annSrc.getSoftware().getName().equalsIgnoreCase(properties.getProperty(SOFTWARE_NAME_PROPNAME)) ||
                !annSrc.getSoftware().getVersion().equalsIgnoreCase(properties.getProperty(SOFTWARE_VERSION_PROPNAME))) {
            reportBuilder.addMessage("Software should not be changed when editing Annotation Source!");
            isValid = false;
        }

        if (!annSrc.getOrganism().getName().equalsIgnoreCase(properties.getProperty(ORGANISM_PROPNAME))) {
            reportBuilder.addMessage("Organism should not be changed when editing Annotation Source!");
            isValid = false;
        }
        return isValid;
    }

    @Override
    protected Collection<String> getRequiredProperties() {
        List<String> propertyNames = new ArrayList<String>(BM_PROPNAMES);
        propertyNames.addAll(PROPNAMES);
        return Collections.unmodifiableCollection(propertyNames);
    }


    @Override
    public boolean isNewAnnSrcUnique(String text, ValidationReportBuilder reportBuilder) {
        final AnnotationSourceProperties properties = AnnotationSourceProperties.createPropertiesFromText(text);
        Software software = new Software(properties.getProperty(SOFTWARE_NAME_PROPNAME), properties.getProperty(SOFTWARE_VERSION_PROPNAME));
        final String onlineVersion = fetchOnLineVersion(properties);
        if (!software.getVersion().equalsIgnoreCase(onlineVersion)) {
            reportBuilder.addMessage("Software version " + software.getVersion() + " does not corresponds to on-line version "
                    + onlineVersion);
            return false;
        }

        if (annSrcDAO.findBioMartAnnotationSource(software.getName(), software.getVersion(),
                properties.getProperty(ORGANISM_PROPNAME)) != null) {
            reportBuilder.addMessage("Annotation source with software " + software.getName() + "/" +
                    software.getVersion() + " for Organism " + properties.getProperty(ORGANISM_PROPNAME) +
                    " already exists. If you need to " +
                    "change it use Edit button");
            return false;
        }
        return true;
    }

    @Override
    protected void extraValidation(AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) {
        validateMartURL(properties, reportBuilder);
    }

    private void validateMartURL(AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) {
        try {
            final String onlineVersion = fetchOnLineVersion(properties);
            System.out.println("onlineVersion = " + onlineVersion);
        } catch (Exception e) {
            reportBuilder.addMessage("One of the properties is invalid: \n"
                    + URL_PROPNAME + ": " + properties.getProperty(URL_PROPNAME) + " \n"
                    + DATABASE_NAME_PROPNAME + ": " + properties.getProperty(DATABASE_NAME_PROPNAME) + " \n"
                    + DATASET_NAME_PROPNAME + ": " + properties.getProperty(DATASET_NAME_PROPNAME) + " \n"
            );
        }
    }

    private String fetchOnLineVersion(AnnotationSourceProperties properties) {
        return martVersionFinder.fetchOnLineVersion(
                properties.getProperty(URL_PROPNAME),
                properties.getProperty(DATABASE_NAME_PROPNAME),
                properties.getProperty(DATASET_NAME_PROPNAME));
    }
}

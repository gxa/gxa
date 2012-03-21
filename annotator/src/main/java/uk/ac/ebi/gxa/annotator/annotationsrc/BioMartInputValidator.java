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

import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.microarray.atlas.model.Organism;
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
public class BioMartInputValidator extends AnnotationSourceInputValidator<BioMartAnnotationSource> {
    @Override
    protected void validateStableFields(BioMartAnnotationSource annSrc, AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) {
        Organism organism = organismDAO.getOrCreateOrganism(properties.getProperty(ORGANISM_PROPNAME));
        Software software = softwareDAO.findOrCreate(properties.getProperty(SOFTWARE_NAME_PROPNAME), properties.getProperty(SOFTWARE_VERSION_PROPNAME));

        if (!annSrc.getSoftware().equals(software)) {
            reportBuilder.addMessage("Software should not be changed when editing Annotation Source!");
        }

        if (!annSrc.getOrganism().equals(organism)) {
            reportBuilder.addMessage("Organism should not be changed when editing Annotation Source!");
        }
    }

    @Override
    protected Collection<String> getRequiredProperties() {
        List<String> propertyNames = new ArrayList<String>(BM_PROPNAMES);
        propertyNames.addAll(PROPNAMES);
        return Collections.unmodifiableCollection(propertyNames);
    }
}

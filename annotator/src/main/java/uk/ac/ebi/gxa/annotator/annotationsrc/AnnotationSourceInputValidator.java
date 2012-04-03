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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.TYPES_PROPNAME;
import static uk.ac.ebi.gxa.annotator.annotationsrc.AnnotationSourceProperties.URL_PROPNAME;

public abstract class AnnotationSourceInputValidator<T extends AnnotationSource> {

    @Autowired
    protected AnnotationSourceDAO annSrcDAO;
    @Autowired
    protected BioEntityTypeDAO typeDAO;
    @Autowired
    protected OrganismDAO organismDAO;
    @Autowired
    protected SoftwareDAO softwareDAO;


    public boolean isValidInputText(String text, ValidationReportBuilder reportBuilder) throws AnnotationLoaderException {

        AnnotationSourceProperties properties = AnnotationSourceProperties.createPropertiesFromText(text);

        validateRequiredFields(properties, reportBuilder);
        validateURL(properties, reportBuilder);
        validateTypes(properties, reportBuilder);

        extraValidation(properties, reportBuilder);
        return reportBuilder.isEmpty();
    }

    protected abstract void extraValidation(AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder);

    protected void validateRequiredFields(AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) {
        List<String> propertyNames = new ArrayList<String>(getRequiredProperties());

        for (String propertyName : propertyNames) {
            final String property = properties.getProperty(propertyName);
            if (Strings.isNullOrEmpty(property)) {
                reportBuilder.addMessage("Required property " + propertyName + " is missing");
            }
        }
    }

    protected void validateTypes(AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) {
        String typesString = properties.getProperty(TYPES_PROPNAME);
        if (Strings.isNullOrEmpty(typesString)) {
            reportBuilder.addMessage("Required property \"types\" is missing");
            return;
        }

        boolean valid = true;
        for (String typeName : properties.getListPropertiesOfType(TYPES_PROPNAME)) {
            try {
                typeDAO.getByName(typeName);
            } catch (RecordNotFoundException e) {
                reportBuilder.addMessage("Unknown bioentity type " + typeName);
                valid = false;
            }
        }

        if (!valid) {
            final List<BioEntityType> all = typeDAO.getAll();
            reportBuilder.addMessage("Valid bioentity types are: " +
                    StringUtils.collectionToCommaDelimitedString(Collections2.transform(all, new Function<BioEntityType, String>() {
                        @Override
                        public String apply(BioEntityType bioEntityType) {
                            return bioEntityType.getName();
                        }
                    })));
        }
    }

    protected void validateURL(AnnotationSourceProperties properties, ValidationReportBuilder reportBuilder) {
        final String urlString = properties.getProperty(URL_PROPNAME);
        try {
            final URI uri = new URI(urlString);
            uri.toURL();
        } catch (Exception e) {
            reportBuilder.addMessage("Invalid software url");
        }
    }

    protected abstract boolean isImmutableFieldsValid(T annSrc, String text, ValidationReportBuilder reportBuilder);

    protected abstract Collection<String> getRequiredProperties();

    public abstract boolean isNewAnnSrcUnique(String text, ValidationReportBuilder reportBuilder);

}
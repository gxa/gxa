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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.apache.http.client.HttpClient;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.AnnotationSourcePropertiesValidator;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.io.IOException;
import java.util.Collection;

/**
 * User: nsklyar
 * Date: 19/01/2012
 */
public class MartPropertiesValidator implements AnnotationSourcePropertiesValidator<BioMartAnnotationSource> {

    private final HttpClient httpClient;

    public MartPropertiesValidator(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void validatePropertyNames(BioMartAnnotationSource annSrc, ValidationReportBuilder reportBuilder) {

        try {
            MartServiceClient martService = MartServiceClientImpl.create(httpClient, annSrc);

            final Collection<String> attributes = martService.runAttributesQuery();
            for (String property : annSrc.getExternalPropertyNames()) {
                if (!attributes.contains(property)) {
                    reportBuilder.addMessage("Invalid external property name: '" + property + "'");
                }
            }

            for (String arrayDesign : annSrc.getExternalArrayDesignNames()) {
                if (!attributes.contains(arrayDesign)) {
                    reportBuilder.addMessage("Invalid external array design name: '" + arrayDesign + "'");
                }
            }

            if (!martService.runDatasetListQuery().contains(annSrc.getDatasetName())) {
                reportBuilder.addMessage("Invalid dataset name: '" + annSrc.getDatasetName() + "'");
            }

            validateMySqlProperties(annSrc, reportBuilder);

        } catch (BioMartException e) {
            throw LogUtil.createUnexpected("Problem when validating annotation source " + annSrc.getName(), e);
        } catch (IOException e) {
            throw LogUtil.createUnexpected("Problem when validating annotation source " + annSrc.getName(), e);
        }
    }

    private void validateMySqlProperties(BioMartAnnotationSource annSrc, ValidationReportBuilder errors) {
        try {
            new BioMartDbDAO(annSrc.getMySqlDbUrl()).testConnection(annSrc.getMySqlDbName(), annSrc.getSoftware().getVersion());
        } catch (BioMartException e) {
            errors.addMessage(e.getMessage());
        }
    }
}

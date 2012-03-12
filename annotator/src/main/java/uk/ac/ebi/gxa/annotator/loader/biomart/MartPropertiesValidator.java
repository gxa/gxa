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

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import uk.ac.ebi.gxa.annotator.validation.AnnotationSourcePropertiesValidator;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

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
    public void getInvalidPropertyNames(BioMartAnnotationSource annSrc, ValidationReportBuilder reportBuilder) {

        try {
            MartServiceClient martService = MartServiceClientImpl.create(httpClient, annSrc);

            final Collection<String> attributes = martService.runAttributesQuery();
            for (String property : annSrc.getExternalPropertyNames()) {
                if (!attributes.contains(property)) {
                    reportBuilder.addMessage(property);
                }
            }

            for (String arrayDesign : annSrc.getExternalArrayDesignNames()) {
                if (!attributes.contains(arrayDesign)) {
                    reportBuilder.addMessage(arrayDesign);
                }
            }

            if (!martService.runDatasetListQuery().contains(annSrc.getDatasetName())) {
                reportBuilder.addMessage(annSrc.getDatasetName());
            }

            final String validationMessage = validateMySqlProperties(annSrc.getMySqlDbUrl(), annSrc.getMySqlDbName(), annSrc.getSoftware().getVersion());
            if (!validationMessage.isEmpty()) {
                reportBuilder.addMessage("Invalid MySQLDb Connection: " + validationMessage);
            }

        } catch (BioMartException e) {
            throw LogUtil.createUnexpected("Problem when validating annotation source " + annSrc.getName(), e);
        } catch (IOException e) {
            throw LogUtil.createUnexpected("Problem when validating annotation source " + annSrc.getName(), e);
        }

    }

    private String validateMySqlProperties(String url, String dbName, String software) {
        BioMartDbDAO dao = new BioMartDbDAO(url);
        return dao.validateConnection(dbName, software);
    }
}

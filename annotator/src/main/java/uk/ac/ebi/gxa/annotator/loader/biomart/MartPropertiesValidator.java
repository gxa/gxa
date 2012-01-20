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
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.loader.AnnotationSourcePropertiesValidator;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.io.IOException;
import java.net.URISyntaxException;
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
    public Collection<String> getInvalidPropertyNames(BioMartAnnotationSource annSrc) {
        Collection<String> missingProperties = new HashSet<String>();

        try {
            MartServiceClient martService = MartServiceClientImpl.create(httpClient, annSrc);

            final Collection<String> attributes = martService.runAttributesQuery();
            for (String property : annSrc.getExternalPropertyNames()) {
                if (!attributes.contains(property)) {
                    missingProperties.add(property);
                }
            }

            for (String arrayDesign : annSrc.getExternalArrayDesignNames()) {
                if (!attributes.contains(arrayDesign)) {
                    missingProperties.add(arrayDesign);
                }
            }

            if (!martService.runDatasetListQuery().contains(annSrc.getDatasetName())) {
                missingProperties.add(annSrc.getDatasetName());
            }

        } catch (URISyntaxException e) {
            throw LogUtil.createUnexpected("Problem when validating annotation source " + annSrc.getName(), e);
        } catch (BioMartException e) {
            throw LogUtil.createUnexpected("Problem when validating annotation source " + annSrc.getName(), e);
        } catch (IOException e) {
            throw LogUtil.createUnexpected("Problem when validating annotation source " + annSrc.getName(), e);
        }
        
        return missingProperties;
    }
}

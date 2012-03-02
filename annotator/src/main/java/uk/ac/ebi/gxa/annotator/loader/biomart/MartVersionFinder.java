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
import uk.ac.ebi.gxa.annotator.loader.AnnotatorFactory;
import uk.ac.ebi.gxa.annotator.loader.VersionFinder;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * User: nsklyar
 * Date: 23/01/2012
 */
public class MartVersionFinder implements VersionFinder<BioMartAnnotationSource>{

    @Autowired
    private HttpClient httpClient;

    @Override
    public String fetchOnLineVersion(BioMartAnnotationSource annSrc) {

        try {
            AnnotatorFactory.setProxyIfExists(httpClient);
            MartServiceClientImpl martClient = MartServiceClientImpl.create(httpClient, annSrc);
            final String database = martClient.getMartLocation().getDatabase();
            return database.substring(database.lastIndexOf("_") + 1);
        } catch (BioMartException e) {
            throw LogUtil.createUnexpected("Problem when fetch on-line version for annotation source " + annSrc.getName(), e);
        } catch (IOException e) {
            throw LogUtil.createUnexpected("Problem when fetch on-line version for annotation source " + annSrc.getName(), e);
        }
    }
}

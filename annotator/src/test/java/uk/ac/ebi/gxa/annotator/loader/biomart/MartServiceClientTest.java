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
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

/**
 * User: nsklyar
 * Date: 19/01/2012
 */
public class MartServiceClientTest {
    @Test
    public void testRunAttributesQuery() throws Exception {
        MartServiceClient martServiceClient = initClient();
        final Collection<String> result = martServiceClient.runAttributesQuery();
        assertTrue(result.size() > 0);
    }

    @Test
    public void testRunDatasetListQuery() throws Exception {
        MartServiceClient martServiceClient = initClient();
        final Collection<String> result = martServiceClient.runDatasetListQuery();
        assertTrue(result.size() > 0);
    }

    private MartServiceClient initClient() throws URISyntaxException {
        HttpClient httpClient = new DefaultHttpClient();
        return new MartServiceClientImpl(httpClient, "http://plants.ensembl.org/biomart/martservice?", "plants", "athaliana_eg_gene");
    }

}

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
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import static com.google.common.io.Closeables.closeQuietly;
import static java.util.Arrays.asList;
import static uk.ac.ebi.gxa.annotator.loader.util.HttpClientHelper.*;

/**
 * Ensembl uses biomart software v 0.7; but the latest is 0.8. The REST API changed significantly in v 0.8,
 * however the new Java API released for it. As soon as ensembl is moved to biomart 0.8 we could probably throw
 * our code away and try to use the Java API from biomart.
 * <p/>
 * Note: the user manuals for martService could be found in http://www.biomart.org/martservice.html
 *
 * @author Olga Melnichuk
 */
class MartServiceClientImpl implements MartServiceClient {

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private final URI martUri;
    private final String databaseName;
    private final String datasetName;

    private final HttpClient httpClient;
    private MartRegistry.MartUrlLocation martLocation;

    public MartServiceClientImpl(HttpClient httpClient, String martUrl, String databaseName, String datasetName) {
        this.martUri = URI.create(martUrl);
        this.databaseName = databaseName;
        this.datasetName = datasetName;
        this.httpClient = httpClient;
    }

    @Override
    public InputStream runQuery(Collection<String> attributes) throws BioMartException, IOException {
        return runQuery(
                new MartQuery(
                        getVirtualSchemaName(),
                        datasetName)
                        .addAttributes(attributes));
    }

    @Override
    public Collection<String> runAttributesQuery() throws BioMartException, IOException {
        final InputStream inputStream = httpPost(httpClient, martUri,
                asList(new BasicNameValuePair("type", "attributes"), new BasicNameValuePair("dataset", datasetName)));
        return MartAttributes.parseAttributes(inputStream);
    }

    @Override
    public Collection<String> runDatasetListQuery() throws BioMartException, IOException {
        final InputStream inputStream = httpPost(httpClient, martUri, asList(new BasicNameValuePair("type", "datasets"), new BasicNameValuePair("mart", getMartName())));
        return MartAttributes.parseDataSets(inputStream);
    }

    private InputStream runQuery(MartQuery query) throws BioMartException, IOException {
        log.debug(query.toString());
        log.debug(martUri.toString());
        return httpPost(httpClient, martUri, asList(new BasicNameValuePair("query", query.toString())));
    }


    private String getVirtualSchemaName() throws BioMartException, IOException {
        return getMartLocation().getVirtualSchema();
    }

    private String getMartName() throws BioMartException, IOException {
        return getMartLocation().getName();
    }

    MartRegistry.MartUrlLocation getMartLocation() throws IOException, BioMartException {
        if (martLocation == null) {
            MartRegistry registry = fetchRegistry();
            martLocation = registry.find(databaseName + "_mart_");
            if (martLocation == null) {
                throw new BioMartException("Could not find MartURLLocation for: martUri = " + martUri +
                        ", database = " + databaseName + ", dataset = " + datasetName);
            }
        }
        return martLocation;
    }

    private MartRegistry fetchRegistry() throws IOException, BioMartException {
        InputStream in = null;
        try {
            in = httpGet(httpClient, getRegistryUri());
            return MartRegistry.parse(in);
        } catch (SAXException e) {
            throw new BioMartException("Failed to parseAttributes BioMart registry response", e);
        } catch (ParserConfigurationException e) {
            throw new BioMartException("Failed to parseAttributes BioMart registry response", e);
        } finally {
            closeQuietly(in);
        }
    }

    private URI getRegistryUri() {
        return concatUri(martUri, asList(
                new BasicNameValuePair("type", "registry")));
    }

    public static MartServiceClientImpl create(HttpClient httpClient, BioMartAnnotationSource annSrc){
        return new MartServiceClientImpl(httpClient, annSrc.getUrl(), annSrc.getDatabaseName(), annSrc.getDatasetName());
    }

    public static MartServiceClientImpl create(HttpClient httpClient, String martUrl, String databaseName, String datasetName){
        return new MartServiceClientImpl(httpClient, martUrl, databaseName, datasetName);
    }
}

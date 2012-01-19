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


import org.apache.commons.fileupload.util.Streams;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.xml.sax.SAXException;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import javax.xml.parsers.ParserConfigurationException;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;
import static java.util.Arrays.asList;

/**
 * Ensembl uses biomart software v 0.7; but the latest is 0.8. The REST API changed significantly in v 0.8,
 * however the new Java API released for it. As soon as ensembl is moved to biomart 0.8 we could probably throw
 * our code away and try to use the Java API from biomart.
 * <p/>
 * Note: the user manuals for martService could be found in http://www.biomart.org/martservice.html
 *
 * @author Olga Melnichuk
 */
class MartServiceClient {

    private final URI martUri;
    private final String databaseName;
    private final String datasetName;

    private final HttpClient httpClient;
    private MartRegistry.MartUrlLocation martLocation;

    public MartServiceClient(HttpClient httpClient, String martUrl, String databaseName, String datasetName) throws URISyntaxException {
        this.martUri = new URI(martUrl);
        this.databaseName = databaseName;
        this.datasetName = datasetName;
        this.httpClient = httpClient;
    }

    public InputStream runQuery(Collection<String> attributes) throws BioMartException, IOException {
        return runQuery(
                new MartQuery(
                        getVirtualSchemaName(),
                        datasetName)
                        .addAttributes(attributes));
    }

    public int runCountQuery(Collection<String> attributes) throws BioMartException, IOException {
        return parseCount(runQuery(
                new MartQuery(
                        getVirtualSchemaName(),
                        datasetName)
                        .addAttributes(attributes)
                        .setCount(true)));
    }

    private InputStream runQuery(MartQuery query) throws BioMartException, IOException {
        return httpPost(martUri, asList(new BasicNameValuePair("query", query.toString())));
    }

    private InputStream httpGet(URI uri) throws IOException, BioMartException {
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpget);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new BioMartException("Server returned invalid response: [status_code = " + statusCode + "; url = " + uri + "]");
        }
        return response.getEntity().getContent();
    }

    private InputStream httpPost(URI uri, List<? extends NameValuePair> params) throws IOException, BioMartException {
        HttpPost httppost = new HttpPost(uri);
        httppost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
        HttpResponse response = httpClient.execute(httppost);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new BioMartException("Server returned invalid response: [status_code = " + statusCode + "; url = " + uri + "]");
        }
        return response.getEntity().getContent();
    }

    private int parseCount(InputStream in) throws IOException, BioMartException {
        try {
            String str = (Streams.asString(in)).trim();
            if (str.matches("\\d+")) {
                return Integer.parseInt(str);
            }
            throw new BioMartException("Bad server response: [" + str + "]");
        } finally {
            closeQuietly(in);
        }
    }

    private String getVirtualSchemaName() throws BioMartException, IOException {
        if (martLocation == null) {
            MartRegistry registry = fetchRegistry();
            martLocation = registry.find(databaseName + "_mart_");
            if (martLocation == null) {
                throw new BioMartException("Could not find MartURLLocation for: martUri = " + martUri +
                        ", database = " + databaseName + ", dataset = " + datasetName);
            }
        }
        return martLocation.getVirtualSchema();
    }

    private MartRegistry fetchRegistry() throws IOException, BioMartException {
        InputStream in = null;
        try {
            in = httpGet(getRegistryUri());
            return MartRegistry.parse(in);
        } catch (SAXException e) {
            throw new BioMartException("Failed to parse BioMart registry response", e);
        } catch (ParserConfigurationException e) {
            throw new BioMartException("Failed to parse BioMart registry response", e);
        } finally {
            closeQuietly(in);
        }
    }

    private URI getRegistryUri() {
        return concatUri(martUri, asList(
                new BasicNameValuePair("type", "registry")));
    }

    private URI concatUri(URI url, final List<? extends NameValuePair> params) {
        List<NameValuePair> q = new ArrayList<NameValuePair>();
        q.addAll(URLEncodedUtils.parse(url, HTTP.UTF_8));
        q.addAll(params);

        try {
            return URIUtils.createURI(
                    url.getScheme(),
                    url.getHost(),
                    url.getPort(),
                    url.getPath(),
                    URLEncodedUtils.format(q, HTTP.UTF_8),
                    "");
        } catch (URISyntaxException e) {
            throw LogUtil.createUnexpected("Failed to re-assemble BioMart url: origin = " + url + ", params = " + params, e);
        }
    }

    public static MartServiceClient create(HttpClient httpClient, BioMartAnnotationSource annSrc) throws URISyntaxException {
        return new MartServiceClient(httpClient, annSrc.getUrl(), annSrc.getDatabaseName(), annSrc.getDatasetName());
    }
}

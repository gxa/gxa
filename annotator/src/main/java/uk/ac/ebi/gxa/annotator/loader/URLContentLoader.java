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

package uk.ac.ebi.gxa.annotator.loader;

import com.google.common.base.Strings;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.AnnotationException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 20/12/2011
 */
public class URLContentLoader {

    static final private Logger log = LoggerFactory.getLogger(URLContentLoader.class);

    private static final String PROXY_HOST = "http.proxyHost";
    private static final String PROXY_PORT = "http.proxyPort";

    private final HttpClient httpClient;

    public URLContentLoader(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public File getContentAsFile(String url, File file) throws AnnotationException {
        //ToDo: check if url is not file

       HttpGet httpGet = new HttpGet(url);
        final HttpParams params = new BasicHttpParams();
        HttpClientParams.setRedirecting(params, true);
        httpGet.setParams(params);

        FileOutputStream out = null;
        try {
            setProxyIfExists(httpClient);
            HttpResponse response = httpClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new AnnotationException("Failed to connect to: " + url + " " + response.getStatusLine());
            }

            HttpEntity entity = response.getEntity();
            final long responseContentLength = entity.getContentLength();
            out = new FileOutputStream(file);
            entity.writeTo(out);
            out.close();

            final long actualLength = file.length();
            if (actualLength < responseContentLength) {
                log.error("Not all data are loaded actual size {} expected size {}", actualLength, responseContentLength);
                throw new AnnotationException("Failed to download all annotation data from: " + url +
                        " expected size=" + responseContentLength + " actual=" + actualLength + ". Please try again!");
            }
        } catch (IOException e) {
            throw new AnnotationException("Fatal transport error when reading from " + url, e);
        } finally {
            closeQuietly(out);
        }
        return file;
    }

    private void setProxyIfExists(HttpClient httpClient) throws AnnotationException {
        String proxyHost = System.getProperty(PROXY_HOST);
        String proxyPort = System.getProperty(PROXY_PORT);
        if (!Strings.isNullOrEmpty(proxyHost) && !Strings.isNullOrEmpty(proxyPort)) {
            try {
                int port = Integer.parseInt(proxyPort);
                final HttpHost proxy = new HttpHost(proxyHost, port, "http");
                httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            } catch (NumberFormatException nfe) {
                throw new AnnotationException("Non-integer proxy port: " + proxyPort + "for proxy host: " + proxyHost);
            }
        }
    }
}

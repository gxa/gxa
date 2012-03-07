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
import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.http.client.HttpClient;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.FileBasedAnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.AnnotationSourcePropertiesValidator;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;

/**
 * User: nsklyar
 * Date: 03/01/2012
 */

public class AnnotatorFactory {
    private static final String PROXY_HOST = "http.proxyHost";
    private static final String PROXY_PORT = "http.proxyPort";

    @Autowired
    private AtlasBioEntityDataWriter beDataWriter;
    @Autowired
    private AnnotationSourceDAO annSrcDAO;
    @Autowired
    private BioEntityPropertyDAO propertyDAO;
    @Autowired
    private HttpClient httpClient;

    public BioMartAnnotator createBioMartAnnotator(BioMartAnnotationSource annSrc) {
        setProxyIfExists(httpClient);
        return new BioMartAnnotator(annSrc, annSrcDAO, propertyDAO, beDataWriter, httpClient);
    }

    public <T extends FileBasedAnnotationSource> FileBasedAnnotator createFileBasedAnnotator(T annSrc) {
        setProxyIfExists(httpClient);
        return new FileBasedAnnotator(annSrc, beDataWriter, httpClient);
    }

    public static void setProxyIfExists(HttpClient httpClient) {
        String proxyHost = System.getProperty(PROXY_HOST);
        String proxyPort = System.getProperty(PROXY_PORT);
        if (!Strings.isNullOrEmpty(proxyHost) && !Strings.isNullOrEmpty(proxyPort)) {
            try {
                int port = Integer.parseInt(proxyPort);
                final HttpHost proxy = new HttpHost(proxyHost, port, "http");
                httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            } catch (NumberFormatException nfe) {
                // queisce
            }
        }
    }
}

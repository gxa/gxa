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

package uk.ac.ebi.gxa.web;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.client.HttpClient;

import java.net.ProxySelector;

/**
 * @author Olga Melnichuk
 */
public class HttpClientFactoryBean {

    private ClientConnectionManager connManager;

    public void setConnManager(ClientConnectionManager connManager) {
        this.connManager = connManager;
    }

    public HttpClient createInstance() {
        return applyProxySettings(new DefaultHttpClient(connManager));
    }

    private static DefaultHttpClient applyProxySettings(DefaultHttpClient httpClient) {
        ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(
                httpClient.getConnectionManager().getSchemeRegistry(),
                ProxySelector.getDefault());
        httpClient.setRoutePlanner(routePlanner);
        return httpClient;
    }
}

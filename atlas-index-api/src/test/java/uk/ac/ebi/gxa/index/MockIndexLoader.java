/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.index;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.core.CoreContainer;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

/**
 * @author pashky
 */
public class MockIndexLoader {
    private static void loadSolrDump(CoreContainer container, String core, String dump) throws SolrServerException, IOException, TransformerException {
        SolrServer solr  = new EmbeddedSolrServer(container, core);


        Source source = new StreamSource(MockIndexLoader.class.getClassLoader().getResourceAsStream("META-INF/" + dump));
        Source xslt = new StreamSource(MockIndexLoader.class.getClassLoader().getResourceAsStream("META-INF/dumpconverter.xslt"));

        StringWriter sw = new StringWriter();
        Result result = new StreamResult(sw);
        TransformerFactory transfactory = TransformerFactory.newInstance();
        Transformer transformer = transfactory.newTransformer(xslt);
        transformer.transform(source, result);

        DirectXmlRequest request = new DirectXmlRequest("/update", sw.toString());
        solr.request(request);
        
        solr.optimize();
        solr.commit();
    }
    
    protected static void populateTemporarySolr(CoreContainer container) throws Exception {
        loadSolrDump(container, "atlas", "dump-atlas.xml");
        loadSolrDump(container, "expt", "dump-expt.xml");
        loadSolrDump(container, "properties", "dump-properties.xml");
    }
}

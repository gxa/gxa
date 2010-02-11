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

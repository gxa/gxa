package uk.ac.ebi.ae3.indexbuilder;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.core.CoreContainer;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

import uk.ac.ebi.gxa.utils.FileUtil;

/**
 * @author pashky
 */
public abstract class BaseAbstractIndexTest{
    private static File solrPath;

    public static File getSolrPath() {
        return solrPath;
    }

    private static void loadSolrDump(CoreContainer container, String core, String dump) throws SolrServerException, IOException, TransformerException {
        SolrServer solr  = new EmbeddedSolrServer(container, core);


        Source source = new StreamSource(BaseAbstractIndexTest.class.getClassLoader().getResourceAsStream("META-INF/" + dump));
        Source xslt = new StreamSource(BaseAbstractIndexTest.class.getClassLoader().getResourceAsStream("META-INF/dumpconverter.xslt"));

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
    

    protected static void deployTemporarySolr() throws Exception {
        solrPath = FileUtil.createTempDirectory("solr");

        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "solr.xml", solrPath, "solr.xml");

        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "solrconfig.xml", solrPath, "atlas/conf/solrconfig.xml");
        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "stopwords.txt", solrPath, "atlas/conf/stopwords.txt");
        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "protwords.txt", solrPath, "atlas/conf/protwords.txt");
        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "synonyms.txt", solrPath, "atlas/conf/synonyms.txt");
        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "schema-atlas.xml", solrPath, "atlas/conf/schema.xml");
        FileUtil.createDirectory(solrPath, "atlas/data");

        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "solrconfig.xml", solrPath, "expt/conf/solrconfig.xml");
        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "stopwords.txt", solrPath, "expt/conf/stopwords.txt");
        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "protwords.txt", solrPath, "expt/conf/protwords.txt");
        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "synonyms.txt", solrPath, "expt/conf/synonyms.txt");
        FileUtil.writeFileFromResource(BaseAbstractIndexTest.class, "schema-expt.xml", solrPath, "expt/conf/schema.xml");
        FileUtil.createDirectory(solrPath, "expt/data");
    }

    protected static void populateTemporarySolr() throws Exception {
        CoreContainer container = new CoreContainer(solrPath.toString(), new File(solrPath, "solr.xml"));

        loadSolrDump(container, "atlas", "dump-atlas.xml");
        loadSolrDump(container, "expt", "dump-expt.xml");

        container.shutdown();
    }

    protected static void removeTemporarySolr() throws Exception {
        FileUtil.deleteDirectory(solrPath);
    }
}

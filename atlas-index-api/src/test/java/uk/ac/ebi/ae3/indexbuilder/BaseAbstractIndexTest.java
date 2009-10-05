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

/**
 * @author pashky
 */
public abstract class BaseAbstractIndexTest{
    private static File solrPath;

    public static File getSolrPath() {
        return solrPath;
    }
    
    private static void writeFileFromResource(File solrPath, String resource, String target) {
        File file = new File(solrPath, target);

        File path = file.getParentFile();
        if(!path.exists() && !path.mkdirs())
            throw new RuntimeException("Can't create target directories: " + path);

        InputStream istream = BaseAbstractIndexTest.class.getClassLoader().getResourceAsStream("META-INF/" + resource);
        try {
            FileOutputStream ostream = new FileOutputStream(file);
            byte b[] = new byte[2048];
            int i;
            while((i = istream.read(b)) >= 0)
                ostream.write(b, 0, i);
            ostream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }                                                                 

    private static File createTempDirectory(String prefix) {
        File path;
        int counter = 0;
        do {
            path = new File(System.getProperty("java.io.tmpdir"), prefix + (counter++));
        } while(!path.mkdirs());
        return path;
    }

    private static void deleteDirectory(File dir){
		if(dir.isDirectory()) {
            for (File file : dir.listFiles())
                deleteDirectory(file);
		}
        dir.delete();
	}

    private static void createDirectory(File path, String local) {
        if(!new File(path, local).mkdirs())
            throw new RuntimeException("Can't create temporary directory: " + local + " in :" + path);
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
        solrPath = createTempDirectory("solr");

        writeFileFromResource(solrPath, "solr.xml", "solr.xml");

        writeFileFromResource(solrPath, "solrconfig.xml", "atlas/conf/solrconfig.xml");
        writeFileFromResource(solrPath, "stopwords.txt", "atlas/conf/stopwords.txt");
        writeFileFromResource(solrPath, "protwords.txt", "atlas/conf/protwords.txt");
        writeFileFromResource(solrPath, "synonyms.txt", "atlas/conf/synonyms.txt");
        writeFileFromResource(solrPath, "schema-atlas.xml", "atlas/conf/schema.xml");
        createDirectory(solrPath, "atlas/data");

        writeFileFromResource(solrPath, "solrconfig.xml", "expt/conf/solrconfig.xml");
        writeFileFromResource(solrPath, "stopwords.txt", "expt/conf/stopwords.txt");
        writeFileFromResource(solrPath, "protwords.txt", "expt/conf/protwords.txt");
        writeFileFromResource(solrPath, "synonyms.txt", "expt/conf/synonyms.txt");
        writeFileFromResource(solrPath, "schema-expt.xml", "expt/conf/schema.xml");
        createDirectory(solrPath, "expt/data");
    }

    protected static void populateTemporarySolr() throws Exception {
        CoreContainer container = new CoreContainer(solrPath.toString(), new File(solrPath, "solr.xml"));

        loadSolrDump(container, "atlas", "dump-atlas.xml");
        loadSolrDump(container, "expt", "dump-expt.xml");

        container.shutdown();
    }

    protected static void removeTemporarySolr() throws Exception {
        deleteDirectory(solrPath);
    }
}

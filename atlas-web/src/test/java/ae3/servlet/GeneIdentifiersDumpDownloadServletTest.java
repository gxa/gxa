package ae3.servlet;

import org.junit.Test;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import junit.framework.TestCase;
import ae3.util.AtlasProperties;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * @author ostolop
 */
public class GeneIdentifiersDumpDownloadServletTest extends TestCase {
    final private Logger log = LoggerFactory.getLogger(getClass());

    private SolrCore core;

    @Override
    protected void setUp() throws IOException, ParserConfigurationException, SAXException {
        final String solrIndexLocation = AtlasProperties.getProperty("atlas.solrIndexLocation");
        final CoreContainer multiCore = new CoreContainer(solrIndexLocation, new File(solrIndexLocation, "solr.xml"));

        core = multiCore.getCore("atlas");
    }

    @Override
    protected void tearDown() throws Exception {
        core.close();
        core = null;
    }

    @Test
    public void testDumpGeneIdentifiers() {
        String testDumpFile = System.getProperty("java.io.tmpdir") + File.separator + "gene_identifiers.txt";

        GeneIdentifiersDumpDownloadServlet svt = new GeneIdentifiersDumpDownloadServlet();

        svt.setBasePath(System.getProperty("java.io.tmpdir"));
        svt.setDumpGeneIdsFilename("gene_identifiers.txt");

        svt.dumpGeneIdentifiers(core);

        File dumpFile = new File(testDumpFile);
        assertTrue(dumpFile.exists());

        if(!dumpFile.delete()) {
            log.error("Failed to delete temporary file");
        }
    }
}

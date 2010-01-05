package ae3.servlet;

import org.junit.*;
import static org.junit.Assert.assertTrue;
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
import java.io.FilenameFilter;

import uk.ac.ebi.ae3.indexbuilder.AbstractOnceIndexTest;

/**
 * @author ostolop
 */
public class GeneIdentifiersDumpDownloadServletTest extends AbstractOnceIndexTest {

    final private Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testDumpGeneIdentifiers() {
        String testDumpFile = System.getProperty("java.io.tmpdir") + File.separator + "gene_identifiers.txt";

        GeneIdentifiersDumpDownloadServlet svt = new GeneIdentifiersDumpDownloadServlet();

        svt.setCoreContainer(getContainer());
        svt.setBasePath(System.getProperty("java.io.tmpdir"));
        svt.setDumpGeneIdsFilename("gene_identifiers.txt");

        svt.dumpGeneIdentifiers();

        File dumpFile = new File(testDumpFile);
        assertTrue(dumpFile.exists());

        if(!dumpFile.delete()) {
            log.error("Failed to delete temporary file");
        }
    }
}

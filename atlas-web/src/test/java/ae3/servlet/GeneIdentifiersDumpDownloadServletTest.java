package ae3.servlet;

import org.junit.*;
import static org.junit.Assert.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;

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

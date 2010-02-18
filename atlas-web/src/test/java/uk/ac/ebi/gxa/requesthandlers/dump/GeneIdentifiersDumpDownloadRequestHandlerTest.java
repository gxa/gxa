package uk.ac.ebi.gxa.requesthandlers.dump;

import org.junit.*;
import static org.junit.Assert.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.requesthandlers.dump.GeneIdentifiersDumpDownloadRequestHandler;

/**
 * @author ostolop
 */
public class GeneIdentifiersDumpDownloadRequestHandlerTest extends AbstractOnceIndexTest {

    final private Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testDumpGeneIdentifiers() {
        File testDumpFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "gene_identifiers.txt");

        GeneIdentifiersDumpDownloadRequestHandler svt = new GeneIdentifiersDumpDownloadRequestHandler();

        svt.setCoreContainer(getContainer());
        svt.setDumpGeneIdsFile(testDumpFile);

        svt.dumpGeneIdentifiers();

        assertTrue(testDumpFile.exists());

        if(!testDumpFile.delete()) {
            log.error("Failed to delete temporary file");
        }
    }
}

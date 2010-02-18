package uk.ac.ebi.gxa.requesthandlers.dump;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;

import static org.junit.Assert.*;
import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.requesthandlers.dump.GoogleSitemapXmlRequestHandler;

/**
 * @author ostolop
 */
public class GoogleSitemapXmlRequestHandlerTest extends AbstractOnceIndexTest {
    final private Logger log = LoggerFactory.getLogger(getClass());

    @After
    public void tearDown() {
        // cleanup
        String[] filesToDelete = new File(System.getProperty("java.io.tmpdir")).list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if(name.startsWith("geneSitemap"))
                    return true;

                return false;
            }
        });

        for (String f : filesToDelete) {
            if(!(new File(f).delete()))
                log.error("Couldn't delete temporary file " + f + " in " + System.getProperty("java.io.tmpdir"));
        }
    }

    @Test
    public void testWriteGeneSitemap() {
        GoogleSitemapXmlRequestHandler svt = new GoogleSitemapXmlRequestHandler();
        svt.setCoreContainer(getContainer());

        File sitemapIndexFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "geneSitemapIndex.xml");
        svt.setSitemapIndexFile(sitemapIndexFile);
        svt.writeGeneSitemap();

        assertTrue(sitemapIndexFile.exists());

        File geneSitemap0 = new File(sitemapIndexFile.getParentFile(), "geneSitemap0.xml.gz");
        assertTrue(geneSitemap0.exists());
    }
}

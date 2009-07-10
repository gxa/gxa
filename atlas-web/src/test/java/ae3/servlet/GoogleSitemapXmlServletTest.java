package ae3.servlet;

import org.junit.*;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import ae3.util.AtlasProperties;
import ae3.service.structuredquery.AtlasStructuredQueryService;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;

import static org.junit.Assert.*;
import uk.ac.ebi.ae3.indexbuilder.AbstractOnceIndexTest;

/**
 * @author ostolop
 */
public class GoogleSitemapXmlServletTest extends AbstractOnceIndexTest {
    final private Logger log = LoggerFactory.getLogger(getClass());

    private static CoreContainer container;

    @BeforeClass
    public static  void initContainer() throws Exception {
        container = new CoreContainer(getSolrPath().toString(), new File(getSolrPath(), "solr.xml"));
    }

    @AfterClass
    public static void shutdownContainer() throws Exception {
        container.shutdown();
    }

    private SolrCore core;

    @Before
    public void setUp() throws IOException, ParserConfigurationException, SAXException {
        core = container.getCore("atlas");
    }

    @After
    public void tearDown() {
        core.close();
        core = null;

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
        GoogleSitemapXmlServlet svt = new GoogleSitemapXmlServlet();

        svt.setBasePath(System.getProperty("java.io.tmpdir"));
        svt.writeGeneSitemap(core);

        File geneSitemapIndex = new File(svt.getBasePath() + File.separator + "geneSitemapIndex.xml");
        assertTrue(geneSitemapIndex.exists());

        File geneSitemap0 = new File(svt.getBasePath() + File.separator + "geneSitemap0.xml.gz");
        assertTrue(geneSitemap0.exists());
    }
}

package uk.ac.ebi.gxa.index.builder;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAOTestCase;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * A test case fo assessing whether the DefaultIndexBuilder class initializes correctly and can run a build of the index
 * over some dummy data from the test database.  Note that only very simple checks are run to ensure that some data has
 * gone into the index.  Precise implementations tests should be done on the individual index building services, not
 * this class.  This test is just to ensure clean startup and shutdown of the main index builder.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestDefaultIndexBuilder extends AtlasDAOTestCase {
    private File indexLocation;
    private DefaultIndexBuilder indexBuilder;

    private CoreContainer coreContainer;
    private SolrServer exptServer;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public void setUp() throws Exception {
        super.setUp();

        indexLocation =
                new File("target" + File.separator + "test" + File.separator + "index");

        indexBuilder = new DefaultIndexBuilder();
        indexBuilder.setAtlasDAO(getAtlasDAO());
        indexBuilder.setIndexLocation(indexLocation);
    }

    public void tearDown() throws Exception {
        super.tearDown();

        // shutdown the indexBuilder and coreContainer if its not already been done
        indexBuilder.shutdown();
        if (coreContainer != null) {
            coreContainer.shutdown();
        }

        // delete the index
        if (indexLocation.exists() && !deleteDirectory(indexLocation)) {
//            fail("Failed to delete " + indexLocation.getAbsolutePath());
            // fail is to strict; just log
            log.warn("Failed to delete " + indexLocation.getAbsolutePath());
        }

        indexLocation = null;
        indexBuilder = null;
    }

    public void createSOLRQueryServers() {
        try {
            coreContainer = new CoreContainer();
            File solr_xml = new File(indexLocation, "solr.xml");
            coreContainer.load(indexLocation.getAbsolutePath(), solr_xml);

            // create an embedded solr server for experiments and genes from this container
            exptServer = new EmbeddedSolrServer(coreContainer, "expt");
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        catch (SAXException e) {
            e.printStackTrace();
            fail();
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testStartup() {
        try {
            indexBuilder.startup();

            // check that the atlas index template was unpacked
            File solr_xml = new File(indexLocation, "solr.xml");
            assertTrue("solr.xml doesn't exist", solr_xml.exists());

            // check atlas and expt directories exist
            File atlas = new File(indexLocation, "atlas");
            assertTrue("atlas/ doesn't exist", atlas.exists());
            assertTrue("atlas/ isn't a directory", atlas.isDirectory());
            File expt = new File(indexLocation, "expt");
            assertTrue("expt/ doesn't exist", expt.exists());
            assertTrue("expt/ isn't a directory", expt.isDirectory());

            // now try a repeat startup
            indexBuilder.startup();
        }
        catch (IndexBuilderException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testShutdown() {
        try {
            // try shutdown without startup
            indexBuilder.shutdown();

            // now startup
            indexBuilder.startup();
            // just check shutdown occurs cleanly, without throwing an exception
            indexBuilder.shutdown();
        }
        catch (IndexBuilderException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testBuildIndex() {
        try {
            indexBuilder.startup();

            // run buildIndex
            indexBuilder.buildIndex();

            // now query the index for the stuff that is in the test DB
            createSOLRQueryServers();

            SolrQuery q = new SolrQuery("*:*");
            q.setRows(10);
            q.setFields("");
            q.addSortField("dwe_exp_id", SolrQuery.ORDER.asc);


            QueryResponse queryResponse = exptServer.query(q);
            SolrDocumentList documentList = queryResponse.getResults();

            if (documentList == null || documentList.size() < 1) {
                fail("No experiments available");
            }

            // just check we have 2 experiments - as this is the number in our dataset
            int expected = getDataSet().getTable("A2_EXPERIMENT").getRowCount();
            int actual = documentList.size();
            assertEquals("Wrong number of docs: expected " + expected +
                    ", actual " + actual, expected, actual);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private boolean deleteDirectory(File directory) {
        boolean success = true;
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    success = success && deleteDirectory(file);
                }
                else {
                    success = success && file.delete();
                }
            }
        }
        return success && directory.delete();
    }
}

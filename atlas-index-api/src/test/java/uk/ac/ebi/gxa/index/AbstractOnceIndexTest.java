package uk.ac.ebi.gxa.index;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.apache.solr.core.CoreContainer;

import java.io.File;

/**
 * Abstract test case class loading a temporary SOLR instance from xml dumps provided in the resources
 * once for all the class's tests. Use it only for read-only testing to speed-up.
 * 
 * Use getSolrPath() method to get solr path
 * @author pashky
 */
public abstract class AbstractOnceIndexTest extends BaseAbstractIndexTest {

    private static CoreContainer container;

    public static CoreContainer getContainer() {
        return container;
    }

    @BeforeClass
    public static void installSolr() throws Exception {
        deployTemporarySolr();
        populateTemporarySolr();
        container = new CoreContainer(getSolrPath().toString(), new File(getSolrPath(), "solr.xml"));
        System.gc();
    }

    @AfterClass
    public static void cleanupSolr() throws Exception {
        container.shutdown();
        removeTemporarySolr();
    }
}

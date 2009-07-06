package uk.ac.ebi.ae3.indexbuilder;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Abstract test case class loading a temporary SOLR instance from xml dumps provided in the resources
 * once for all the class's tests. Use it only for read-only testing to speed-up.
 * 
 * Use getSolrPath() method to get solr path
 * @author pashky
 */
public abstract class AbstractOnceIndexTest extends BaseAbstractIndexTest {

    @BeforeClass
    public static void installSolr() throws Exception {
        deployTemporarySolr();
        populateTemporarySolr();
        System.gc();
    }

    @AfterClass
    public static void cleanupSolr() throws Exception {
        removeTemporarySolr();
    }
}

package uk.ac.ebi.gxa.index;

import org.junit.After;
import org.junit.Before;

/**
 * Abstract test case class loading a temporary SOLR instance from xml dumps provided in the resources
 * for EVERY test. Use it for write operations.
 *
 * Use getSolrPath() method to get solr path
 * @author pashky
 */
public abstract class AbstractEveryIndexTest extends BaseAbstractIndexTest {

    @Before
    public void installSolr() throws Exception {
        deployTemporarySolr();
        populateTemporarySolr();
        System.gc();
    }

    @After
    public void cleanupSolr() throws Exception {
        removeTemporarySolr();
    }

}

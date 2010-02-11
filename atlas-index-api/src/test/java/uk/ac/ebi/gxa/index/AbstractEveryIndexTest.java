package uk.ac.ebi.gxa.index;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.apache.solr.core.CoreContainer;

import java.io.File;

import uk.ac.ebi.gxa.utils.FileUtil;

/**
 * Abstract test case class loading a temporary SOLR instance from xml dumps provided in the resources
 * for EVERY test. Use it for write operations.
 *
 * Use getSolrPath() method to get solr path
 * @author pashky
 */
public abstract class AbstractEveryIndexTest {

    private CoreContainer container;
    private File indexPath;

    public CoreContainer getContainer() {
        return container;
    }

    @Before
    public void installSolr() throws Exception {
        indexPath = FileUtil.createTempDirectory("solr");
        SolrContainerFactory factory = new SolrContainerFactory();
        factory.setAtlasIndex(indexPath);
        factory.setTemplatePath("solr");
        container = factory.createContainer();
        MockIndexLoader.populateTemporarySolr(getContainer());
        System.gc();
    }

    @After
    public void cleanupSolr() throws Exception {
        container.shutdown();
        FileUtil.deleteDirectory(indexPath);
    }

}

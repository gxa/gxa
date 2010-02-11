package uk.ac.ebi.gxa.index;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.apache.solr.core.CoreContainer;

import java.io.File;

import uk.ac.ebi.gxa.utils.FileUtil;

/**
 * Abstract test case class loading a temporary SOLR instance from xml dumps provided in the resources
 * once for all the class's tests. Use it only for read-only testing to speed-up.
 * 
 * Use getSolrPath() method to get solr path
 * @author pashky
 */
public abstract class AbstractOnceIndexTest {

    private static CoreContainer container;
    private static File indexPath;

    public static CoreContainer getContainer() {
        return container;
    }

    @BeforeClass
    public static void installSolr() throws Exception {
        indexPath = FileUtil.createTempDirectory("solr");
        SolrContainerFactory factory = new SolrContainerFactory();
        factory.setAtlasIndex(indexPath);
        factory.setTemplatePath("solr");
        container = factory.createContainer();
        MockIndexLoader.populateTemporarySolr(getContainer());
        System.gc();
    }

    @AfterClass
    public static void cleanupSolr() throws Exception {
        container.shutdown();
        FileUtil.deleteDirectory(indexPath);
    }
}

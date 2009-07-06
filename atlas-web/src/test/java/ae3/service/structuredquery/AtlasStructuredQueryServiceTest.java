package ae3.service.structuredquery;

import org.apache.solr.core.CoreContainer;
import org.junit.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import uk.ac.ebi.ae3.indexbuilder.AbstractOnceIndexTest;

import java.io.File;

/**
 * @author pashky
 */
public class AtlasStructuredQueryServiceTest extends AbstractOnceIndexTest {

    private static CoreContainer container;

    @BeforeClass
    public static  void initContainer() throws Exception {
        container = new CoreContainer(getSolrPath().toString(), new File(getSolrPath(), "solr.xml"));
    }

    @AfterClass
    public static void shutdownContainer() throws Exception {
        container.shutdown();
    }

    AtlasStructuredQueryService service;

    @Before
    public void createService()
    {
        service = new AtlasStructuredQueryService(container);
    }

    @After
    public void dropService()
    {
        service = null;
    }

    private static boolean containsString(Iterable iter, String s) {
        for(Object o : iter)
            if(o != null && o.toString().equals(s))
                return true;
        return false;
    }

    @Test
    public void test_getGeneProperties() {
        Iterable<String> gprops = service.getGeneProperties();
        assertTrue(gprops.iterator().hasNext());
        assertTrue(containsString(gprops, "name"));
        assertTrue(containsString(gprops, "identifier"));
        assertTrue(containsString(gprops, "goterm"));        
    }


    @Test
    public void test_doStructuredAtlasQuery() {
        AtlasStructuredQueryResult result = service.doStructuredAtlasQuery(
                new AtlasStructuredQueryBuilder()
                        .andGene("aspm")
                        .toQuery()
        );

        assertNotNull(result);
        assertTrue(result.getSize() > 0);
    }
}

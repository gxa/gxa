package ae3.service.structuredquery;

import org.junit.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import uk.ac.ebi.ae3.indexbuilder.AbstractOnceIndexTest;

/**
 * @author pashky
 */
public class AtlasStructuredQueryServiceTest extends AbstractOnceIndexTest {

    AtlasStructuredQueryService service;

    @Before
    public void createService()
    {
        service = new AtlasStructuredQueryService(getContainer());
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
        Iterable<String> gprops = service.getGenePropertyOptions();
        assertTrue(gprops.iterator().hasNext());
        assertTrue(containsString(gprops, "gene"));
        assertTrue(containsString(gprops, "KEYWORD"));
        assertTrue(containsString(gprops, "GOTERM"));
    }


    @Test
    public void test_doStructuredAtlasQuery() {
        AtlasStructuredQueryResult result = service.doStructuredAtlasQuery(
                new AtlasStructuredQueryBuilder()
                        .andGene("aspm")
                        .query()
        );

        assertNotNull(result);
        assertTrue(result.getSize() > 0);
    }
}

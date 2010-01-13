package ae3.service.structuredquery;

import org.junit.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import ae3.dao.AtlasDao;

/**
 * @author pashky
 */
public class AtlasStructuredQueryServiceTest extends AbstractOnceIndexTest {

    AtlasStructuredQueryService service;

    @Before
    public void createService()
    {
        EmbeddedSolrServer solrServerAtlas = new EmbeddedSolrServer(getContainer(), "atlas");
        EmbeddedSolrServer expt = new EmbeddedSolrServer(getContainer(), "expt");
        EmbeddedSolrServer serverProp = new EmbeddedSolrServer(getContainer(), "properties");

        AtlasDao dao = new AtlasDao();
        dao.setSolrServerAtlas(solrServerAtlas);
        dao.setSolrServerExpt(expt);

        AtlasEfvService efvService = new AtlasEfvService();
        efvService.setSolrServerAtlas(solrServerAtlas);
        efvService.setSolrServerExpt(expt);
        efvService.setSolrServerProp(serverProp);

        AtlasEfoService efoService = new AtlasEfoService();
        efoService.setSolrServerAtlas(solrServerAtlas);

        service = new AtlasStructuredQueryService();
        service.setSolrServerAtlas(solrServerAtlas);
        service.setSolrServerExpt(expt);
        service.setSolrServerProp(serverProp);
        service.setAtlasSolrDAO(dao);
        service.setEfoService(efoService);
        service.setEfvService(efvService);
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

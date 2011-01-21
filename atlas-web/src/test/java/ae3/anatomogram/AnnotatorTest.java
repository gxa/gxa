package ae3.anatomogram;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ae3.service.AtlasStatisticsQueryService;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.index.StatisticsStorageFactory;
import uk.ac.ebi.gxa.statistics.StatisticsStorage;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class AnnotatorTest extends AbstractOnceIndexTest {

    private AtlasSolrDAO atlasSolrDAO;
    private Annotator annotator;


    @Before
    public void createService() throws Exception {
        EmbeddedSolrServer solrServerAtlas = new EmbeddedSolrServer(getContainer(), "atlas");
        atlasSolrDAO = new AtlasSolrDAO();
        atlasSolrDAO.setSolrServerAtlas(solrServerAtlas);

        Efo efo = new Efo();
        efo.setUri(new URI("resource:META-INF/efo.owl"));

        annotator = new Annotator();
        String bitIndexResourceName = "bitstats";
        File bitIndexResourcePath = new File(this.getClass().getClassLoader().getResource(bitIndexResourceName).toURI());
        StatisticsStorageFactory statisticsStorageFactory = new StatisticsStorageFactory(bitIndexResourceName);
        statisticsStorageFactory.setAtlasIndex(new File(bitIndexResourcePath.getParent()));
        StatisticsStorage statisticsStorage = statisticsStorageFactory.createStatisticsStorage();
        AtlasStatisticsQueryService atlasStatisticsQueryService = new AtlasStatisticsQueryService(bitIndexResourceName);
        atlasStatisticsQueryService.setStatisticsStorage(statisticsStorage);
        atlasStatisticsQueryService.setEfo(efo);
        annotator.setAtlasStatisticsQueryService(atlasStatisticsQueryService);
        annotator.setEfo(efo);
        annotator.load(); // load svg templates
    }

    @Test
    public void testGetHasAnatomogram() throws Exception {
        Map<String, Boolean> geneIds = new HashMap<String, Boolean>() {{
            put("ABCDEF", false);
            put("ENSMUSG00000020275", true);
            put(".-_", false);
        }};

        assertTrue(geneIds.entrySet().size() > 0); // At least oene gene of the above genes was found in Solr

        boolean success = false;
        for (Map.Entry<String, Boolean> mapEntry : geneIds.entrySet()) {
            //atlasGene may be null
            AtlasGene atlasGene = atlasSolrDAO.getGeneByIdentifier(mapEntry.getKey()).getGene();

            for (Annotator.AnatomogramType type : new Annotator.AnatomogramType[]{Annotator.AnatomogramType.Web, Annotator.AnatomogramType.Das}) {

                if (null == atlasGene)
                    continue;

                Anatomogram an = annotator.getAnatomogram(type, atlasGene);
                assertEquals(mapEntry.getValue(), !an.isEmpty());
                success = true;
            }
            assertTrue(success);
        }
    }
}

   
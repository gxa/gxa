package ae3.anatomogram;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;
import ae3.service.AtlasStatisticsQueryService;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoNode;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.statistics.StatisticsQueryUtils;
import uk.ac.ebi.gxa.statistics.StatisticsType;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnnotatorTest extends AbstractOnceIndexTest {

    private AtlasSolrDAO atlasSolrDAO;
    private Annotator annotator;
    private List<String> efoTerms;

    @Before
    public void createService() throws Exception {
        long start = System.currentTimeMillis();
        EmbeddedSolrServer solrServerAtlas = new EmbeddedSolrServer(getContainer(), "atlas");
        atlasSolrDAO = new AtlasSolrDAO();
        atlasSolrDAO.setSolrServerAtlas(solrServerAtlas);
        System.out.println("Solr index mocked in: " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();

        // List of efo terms displayable on a mouse anatomogram
        String[] efoTermsArr = {
                "EFO_0000827","EFO_0000793","EFO_0000803","EFO_0000815","EFO_0000265",
                "EFO_0000110","EFO_0000943","EFO_0000792","EFO_0000800","EFO_0000889",
                "EFO_0000934","EFO_0000935","EFO_0000968","EFO_0001385","EFO_0001412",
                "EFO_0001413","EFO_0001937","EFO_0000302" };

        efoTerms = Arrays.asList(efoTermsArr);


        // Mock AtlasStatisticsQueryService object
        AtlasStatisticsQueryService atlasStatisticsQueryService = EasyMock.createMock(AtlasStatisticsQueryService.class);
        for (String efoTermStr : efoTerms) {
            EasyMock.expect(atlasStatisticsQueryService.getExperimentCountsForGene(efoTermStr, StatisticsType.UP, StatisticsQueryUtils.EFO, 169968252l)).andReturn(1);
            EasyMock.expect(atlasStatisticsQueryService.getExperimentCountsForGene(efoTermStr, StatisticsType.DOWN, StatisticsQueryUtils.EFO, 169968252l)).andReturn(1);
        }

        // Mock efo
        Efo efo = EasyMock.createMock(Efo.class);
        for (String efoTermStr : efoTerms) {
            EfoNode efoNode = new EfoNode(efoTermStr, efoTermStr, true, Collections.<String>emptyList());
            EasyMock.expect(efo.getTermById(efoTermStr)).andReturn(new EfoTerm(efoNode, 1, true));
        }

        System.out.println("AtlasStatisticsQueryService and efo mocks created in : " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        annotator = new Annotator();
        annotator.setAtlasStatisticsQueryService(atlasStatisticsQueryService);
        annotator.setEfo(efo);
        annotator.load(); // load svg templates
        System.out.println("Annotator init in: " + (System.currentTimeMillis() - start) + " ms");
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

   
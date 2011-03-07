package uk.ac.ebi.gxa.anatomogram;

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
import uk.ac.ebi.gxa.statistics.Attribute;
import uk.ac.ebi.gxa.statistics.EfoAttribute;
import uk.ac.ebi.gxa.statistics.StatisticsType;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnatomogramFactoryTest extends AbstractOnceIndexTest {

    private AtlasSolrDAO atlasSolrDAO;
    private AnatomogramFactory anatomogramFactory;
    private List<String> efoTerms;
    private long geneId;

    @Before
    public void createService() throws Exception {

        geneId = 169968252l; // ENSMUSG00000020275
        EmbeddedSolrServer solrServerAtlas = new EmbeddedSolrServer(getContainer(), "atlas");
        atlasSolrDAO = new AtlasSolrDAO();
        atlasSolrDAO.setSolrServerAtlas(solrServerAtlas);

        // List of efo terms displayable on a mouse anatomogram
        String[] efoTermsArr = {
                "EFO_0000827","EFO_0000793","EFO_0000803","EFO_0000815","EFO_0000265",
                "EFO_0000110","EFO_0000943","EFO_0000792","EFO_0000800","EFO_0000889",
                "EFO_0000934","EFO_0000935","EFO_0000968","EFO_0001385","EFO_0001412",
                "EFO_0001413","EFO_0001937","EFO_0000302"};
        efoTerms = Arrays.asList(efoTermsArr);

        anatomogramFactory = new AnatomogramFactory();
        anatomogramFactory.load(); // load svg templates
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

            for (AnatomogramFactory.AnatomogramType type : new AnatomogramFactory.AnatomogramType[]{AnatomogramFactory.AnatomogramType.Web, AnatomogramFactory.AnatomogramType.Das}) {

                mockObjects();

                if (null == atlasGene)
                    continue;

                Anatomogram an = anatomogramFactory.getAnatomogram(type, atlasGene);
                assertEquals(mapEntry.getValue(), !an.isEmpty());
                success = true;
            }
            assertTrue(success);
        }
    }


    // Create and populate required functionality of mock objects: atlasStatisticsQueryService and efo
    private void mockObjects() {
        // Mock AtlasStatisticsQueryService object
        AtlasStatisticsQueryService atlasStatisticsQueryService = EasyMock.createMock(AtlasStatisticsQueryService.class);
        for (String efoTermStr : efoTerms) {
            Attribute upAttr = new EfoAttribute(efoTermStr, StatisticsType.UP);
            EasyMock.expect(atlasStatisticsQueryService.getExperimentCountsForGene(upAttr, geneId)).andReturn(1);
            Attribute downAttr = new EfoAttribute(efoTermStr, StatisticsType.UP);
            EasyMock.expect(atlasStatisticsQueryService.getExperimentCountsForGene(downAttr, geneId)).andReturn(1);
        }
        EasyMock.replay(atlasStatisticsQueryService);

        // Mock Efo object
        Efo efo = EasyMock.createMock(Efo.class);
        for (String efoTermStr : efoTerms) {
            EfoNode efoNode = new EfoNode(efoTermStr, efoTermStr, false, Collections.<String>emptyList());
            EfoTerm efoTerm = new EfoTerm(efoNode, 0, false);
            EasyMock.expect(efo.getTermById(EasyMock.eq(efoTermStr))).andReturn(efoTerm);
        }
        EasyMock.replay(efo);
        anatomogramFactory.setAtlasStatisticsQueryService(atlasStatisticsQueryService);
        anatomogramFactory.setEfo(efo);
    }
}

   
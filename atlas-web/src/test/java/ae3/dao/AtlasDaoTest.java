package ae3.dao;

import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import uk.ac.ebi.ae3.indexbuilder.AbstractOnceIndexTest;

import java.util.ArrayList;
import java.util.List;

public class AtlasDaoTest extends AbstractOnceIndexTest
{
    private AtlasDao dao;

    @Before
    public void initDao() {
        dao = new AtlasDao();
        dao.setSolrServerAtlas(new EmbeddedSolrServer(getContainer(), "atlas"));
        dao.setSolrServerExpt(new EmbeddedSolrServer(getContainer(), "expt"));
    }

    @Test
    public void testGetAtlasGene() {
        AtlasDao.AtlasGeneResult atlasGene = dao.getGeneByIdentifier("ENSG00000066279");
        assertNotNull(atlasGene);
        assertTrue(atlasGene.isFound());
        assertFalse(atlasGene.isMulti());
        assertNotNull(atlasGene.getGene());
        assertTrue(atlasGene.getGene().getGeneName().equals("ASPM"));
    }

    @Test
    public void testRetrieveOrthoGenes() {
        AtlasDao.AtlasGeneResult result = dao.getGeneByIdentifier("ENSG00000066279");
        assertNotNull(result);
        assertTrue(result.isFound());
        assertFalse(result.isMulti());
        assertNotNull(result.getGene());

        AtlasGene atlasGene = result.getGene();
        assertNotNull(atlasGene.getOrthologsIds());

        dao.retrieveOrthoGenes(atlasGene);

        assertNotNull(atlasGene.getOrthoGenes());
        ArrayList<AtlasGene> orthos = atlasGene.getOrthoGenes();

        //Test successful retrieval of gene documents from the index corresponding to the gene's list of orthologs
        assertNotNull(orthos);

        for (AtlasGene ortho: orthos){
            String orthoENSid = ortho.getGeneEnsembl();
            //Test retrieved gene documents retrieved to match those in the ortholog list of the gene
            assertTrue("Gene mismatch between gene ortholog ids and the corresponding retrieved genes from index",atlasGene.getOrthologs().contains(orthoENSid));
        }

        //Make sure the gene's id is not listed in its own ortholog list
        assertFalse("Gene's id is listed in its own orthologs list",atlasGene.getOrthologs().contains("ENSG00000066279"));
    }

	@Test
	public void test_getExperimentByIdDw()
	{
		  AtlasExperiment exp = dao.getExperimentById("334420710");
		  assertNotNull(exp);
		  assertNotNull(exp.getAccession());
	}

	@Test	
	public void test_getExperimentByAccession()
	{
		AtlasExperiment exp = dao.getExperimentByAccession("E-MEXP-980");
        assertNotNull(exp);
        assertEquals("E-MEXP-980", exp.getAccession());
	}

    @Test
    public void testGetAtlasGeneMulti() {
        AtlasDao.AtlasGeneResult atlasGene = dao.getGeneByIdentifier("Q9HCN2");
        assertNotNull(atlasGene);
        assertTrue(atlasGene.isFound());
        assertTrue(atlasGene.isMulti());
        assertNotNull(atlasGene.getGene());
    }

    @Test
    public void testGetAtlasGeneUnknown() {
        AtlasDao.AtlasGeneResult atlasGene = dao.getGeneByIdentifier("noName");
        assertNotNull(atlasGene);
        assertFalse(atlasGene.isFound());
        assertFalse(atlasGene.isMulti());
        assertNull(atlasGene.getGene());
    }

    @Test
    public void test_getRankedGeneExperiments() {
        AtlasDao.AtlasGeneResult r = dao.getGeneByIdentifier("ENSG00000066279");

        List<AtlasExperiment> list = dao.getRankedGeneExperiments(r.getGene(), null, null, -1, -1);
        assertNotNull(list);
        assertTrue(list.size() > 0);

        List<AtlasExperiment> list2 = dao.getRankedGeneExperiments(r.getGene(), null, null, 1, 5);
        assertNotNull(list2);
        assertEquals(5, list2.size());

        List<AtlasExperiment> list3 = dao.getRankedGeneExperiments(r.getGene(), "cellline", "BT474", -1, -1);
        assertNotNull(list3);
        assertTrue(list3.size() > 0);
    }
}

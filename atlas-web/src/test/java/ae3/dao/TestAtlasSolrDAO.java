/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package ae3.dao;

import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;

import java.util.List;

public class TestAtlasSolrDAO extends AbstractOnceIndexTest
{
    private AtlasSolrDAO atlasSolrDAO;

    @Before
    public void initDao() {
        atlasSolrDAO = new AtlasSolrDAO();
        atlasSolrDAO.setSolrServerAtlas(new EmbeddedSolrServer(getContainer(), "atlas"));
        atlasSolrDAO.setSolrServerExpt(new EmbeddedSolrServer(getContainer(), "expt"));
    }

    @Test
    public void testGetAtlasGene() {
        AtlasSolrDAO.AtlasGeneResult atlasGene = atlasSolrDAO.getGeneByIdentifier("ENSMUSG00000020275");
        assertNotNull(atlasGene);
        assertTrue(atlasGene.isFound());
        assertFalse(atlasGene.isMulti());
        assertNotNull(atlasGene.getGene());
        assertTrue(atlasGene.getGene().getGeneName().equals("Rel"));
    }

    @Test
    public void testRetrieveOrthoGenes() {
        AtlasSolrDAO.AtlasGeneResult result = atlasSolrDAO.getGeneByIdentifier("ENSMUSG00000020275");
        assertNotNull(result);
        assertTrue(result.isFound());
        assertFalse(result.isMulti());
        assertNotNull(result.getGene());

        AtlasGene atlasGene = result.getGene();
        assertFalse(atlasGene.getOrthologs().isEmpty());

        List<AtlasGene> orthos = atlasSolrDAO.getOrthoGenes(atlasGene);

        //Test successful retrieval of gene documents from the index corresponding to the gene's list of orthologs
        assertNotNull(orthos);

        for (AtlasGene ortho: orthos){
            String orthoid = ortho.getGeneIdentifier();
            //Test retrieved gene documents retrieved to match those in the ortholog list of the gene
            assertTrue("Gene mismatch between gene ortholog ids and the corresponding retrieved genes from index",
                    atlasGene.getOrthologs().contains(orthoid));
        }

        //Make sure the gene's id is not listed in its own ortholog list
        assertFalse("Gene's id is listed in its own orthologs list",atlasGene.getOrthologs().contains("ENSG00000121075"));
    }

	@Test
	public void test_getExperimentByIdDw()
	{
		  AtlasExperiment exp = atlasSolrDAO.getExperimentById("1036804999");
		  assertNotNull(exp);
		  assertNotNull(exp.getAccession());
	}

	@Test	
	public void test_getExperimentByAccession()
	{
		AtlasExperiment exp = atlasSolrDAO.getExperimentByAccession("E-MEXP-2058");
        assertNotNull(exp);
        assertEquals("E-MEXP-2058", exp.getAccession());
	}

    @Test
    public void testGetAtlasGeneUnknown() {
        AtlasSolrDAO.AtlasGeneResult atlasGene = atlasSolrDAO.getGeneByIdentifier("noName");
        assertNotNull(atlasGene);
        assertFalse(atlasGene.isFound());
        assertFalse(atlasGene.isMulti());
        assertNull(atlasGene.getGene());
    }
}

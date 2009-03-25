package ae3.service;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import ae3.model.AtlasGene;

public class AtlasGeneServiceTest {

	@Test
	public void testGetAtlasGene() {
		AtlasGene atlasGene = AtlasGeneService.getAtlasGene("ENSG00000066279");
		assertNotNull(atlasGene);
		assertTrue(atlasGene.getGeneName().equals("ASPM"));
	}

	@Test
	public void testRetrieveOrthoGenes() {
		AtlasGene atlasGene = AtlasGeneService.getAtlasGene("ENSG00000066279");
		assertNotNull(atlasGene);
		assertNotNull(atlasGene.getOrthologsIds());
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

}

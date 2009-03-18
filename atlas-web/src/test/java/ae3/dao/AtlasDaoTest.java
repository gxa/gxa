package ae3.dao;

import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Test;

import ae3.AtlasAbstractTest;
import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;

public class AtlasDaoTest extends AtlasAbstractTest
{
	@Test
	public void test_getExperimentByIdAER() throws AtlasObjectNotFoundException
	{
		  AtlasExperiment exp=AtlasDao.getExperimentByIdAER("1324144279");
		  assertNotNull(exp);
		  assertNotNull(exp.getAerExpAccession());
		  assertNotNull(exp.getAerExpId());
		  assertNotNull(exp.getAerExpName());
		  assertNotNull(exp.getDwExpId());
		  if (exp.getDwExpId() != null)
		  {
			  AtlasExperiment exp1=AtlasDao.getExperimentByIdDw(exp.getDwExpId().toString());
			  assertNotNull(exp1);
			  assertNotNull(exp1.getDwExpAccession());
			  assertNotNull(exp1.getDwExpId());
			  assertNotNull(exp1.getDwExpDescription());			  
		  }

		  log.info("######################## Experiment name: " + exp.getAerExpAccession());
		  log.info("######################## Experiment id: " + exp.getAerExpId());
		  log.info("######################## Experiment id: " + exp.getDwExpId());
		  
	}
	
	@Test
	public void test_getExperimentByIdDw() throws AtlasObjectNotFoundException
	{
		  AtlasExperiment exp=AtlasDao.getExperimentByIdDw("334420710");
		  assertNotNull(exp);
		  assertNotNull(exp.getDwExpAccession());
		  assertNotNull(exp.getAerExpId());
		  assertNotNull(exp.getDwExpType());
		  log.info("######################## Experiment name: " + exp.getDwExpAccession());
		  log.info("######################## Experiment id: " + exp.getAerExpId());
		  log.info("######################## Experiment id: " + exp.getDwExpId());
		  
	}

	@Test	
	public void test_getExperimentByAccession() throws AtlasObjectNotFoundException
	{
		AtlasExperiment exp=AtlasDao.getExperimentByAccession("E-MEXP-980");
	}
	
	@Test
	public void test_getGeneByIdentifier(){
		boolean notFound_thrown = false;
		boolean multi_thrown = false;
		
			
		//Test normal gene query
		try{
			AtlasGene gene = AtlasDao.getGeneByIdentifier("ENSG00000066279");
			assertNotNull(gene);
		}catch (AtlasObjectNotFoundException ex){
			notFound_thrown = true;
		}catch (MultipleGeneException ex){
			multi_thrown = true;
		}
		assertFalse(notFound_thrown);
		assertFalse(multi_thrown);
		
		//Test multiple gene hit exception
		multi_thrown = false;
		notFound_thrown = false;
		try{
			AtlasGene gene2 = AtlasDao.getGeneByIdentifier("P08898");
		}catch (MultipleGeneException ex){
			multi_thrown = true;
		}catch (AtlasObjectNotFoundException ex){
			notFound_thrown = true;
		}
		assertTrue(multi_thrown);
		assertFalse(notFound_thrown);
		
		//Test gene not found exception
		notFound_thrown = false;
		multi_thrown = false;
		try{
			AtlasGene gene2 = AtlasDao.getGeneByIdentifier("mysteriousGene");
		}catch (AtlasObjectNotFoundException ex){
			notFound_thrown = true;
		}catch (MultipleGeneException ex){
			multi_thrown = true;
		}
		assertTrue(notFound_thrown);
		assertFalse(multi_thrown);

		
		
	}
	

	
}

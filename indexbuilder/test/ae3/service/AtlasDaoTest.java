package ae3.service;

import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.model.AtlasExperiment;

public class AtlasDaoTest extends AtlasAbstractTest
{
	public void test_getExperiment1() throws AtlasObjectNotFoundException
	{
		  AtlasExperiment exp=AtlasDao.getExperiment("1324144279");
		  assertNotNull(exp);
		  assertNotNull(exp.getExperimentAccession());
		  assertNotNull(exp.getExperimentId());
		  assertNotNull(exp.getExperimentTypes());
		  log.info("Experiment name: " + exp.getExperimentAccession());
		  log.info("Experiment id: " + exp.getExperimentId());
		  
	}
	
	public void test_getExperimentByAccession() throws AtlasObjectNotFoundException
	{
		AtlasExperiment exp=AtlasDao.getExperimentByAccession("E-MEXP-980");
	}
	
	public void test_getExperiments()
	{
		String keywords[] = {"cancer"};
		List<AtlasExperiment> l=AtlasDao.getExperiments(keywords);
		if (l.size() == 0)
		{
			fail("Experiment not found in index");
		}
		else
		{
			  log.info("################ Number of Experiments: " + l.size());
		}
		
	}
	
	public void test_getExperimentsCount() throws SolrServerException
	{
		String keywords[] = {"cancer"};
		long count=AtlasDao.getExperimentsCount(keywords);
		log.info("############### Number of exp is : " + count);
		
	}
	
}

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
	
	public void test_getExperiments() throws SolrServerException
	{
		String keywords[] = {"cancer"};
		long count = AtlasDao.getExperimentsCount(keywords);
		int start = 10;
		int rows = 0;
		while (rows < count)
		{
		  start = rows;		  
		  rows = rows + 10;
		  List<AtlasExperiment> l=AtlasDao.getExperiments(keywords,start,10);
		  log.info("############### Results: " + l.size());
		}
		
	}
	
	public void test_getExperimentsCount() throws SolrServerException
	{
		String keywords[] = {"cancer"};
		long count=AtlasDao.getExperimentsCount(keywords);
		log.info("############### Number of exp is : " + count);
		
	}
	
}

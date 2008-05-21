package ae3.service;

import java.util.logging.Logger;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Test;

import uk.ac.ebi.ae3.indexbuilder.Constants;

import ae3.AtlasAbstractTest;


import junit.framework.TestCase;

public class ArrayExpressSearchServiceTest extends AtlasAbstractTest
{
	private ArrayExpressSearchService searchservice;
	public ArrayExpressSearchServiceTest()
	{
		// TODO Auto-generated constructor stub
	}

	
	@Test
	public void test_fullTextQueryExpts()
	{
		log.info("Start test searchExptTest");
		String query = Constants.FIELD_AER_SAAT_CAT + ":Organism";
		QueryResponse resp=ArrayExpressSearchService.instance().fullTextQueryExpts(query);
		long count = resp.getResults().getNumFound();
		log.info("####################### Count" + count);
		if (resp != null)
			assertNotNull(resp);
		else
			assertNull(resp);
			
	}
	
	
}

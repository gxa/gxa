package ae3.service;

import java.util.logging.Logger;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Test;


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
		QueryResponse resp=ArrayExpressSearchService.instance().fullTextQueryExpts("E-MEXP-980");
		if (resp != null)
			assertNotNull(resp);
		else
			assertNull(resp);
			
	}
	
	
}

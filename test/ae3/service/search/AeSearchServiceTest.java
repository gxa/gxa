package ae3.service.search;

import org.junit.Test;

import ae3.AtlasAbstractTest;

public class AeSearchServiceTest extends AtlasAbstractTest
{
	@Test
	public void test_getNumberOfDoc() throws Exception
	{
		
		long value = AeSearchService.getNumberOfDoc(null, null, null);
		assertEquals(0, value);
		String keywords = "Mus";
		value=AeSearchService.getNumberOfDoc(keywords, null, null);
		if (value == 0)
			fail("value is 0");
		log.info("######################################## Number of documents" + value);
		String species = "Mus musculus";
		value=AeSearchService.getNumberOfDoc(keywords, species, null);
		if (value == 0)
			fail("value is 0");
		log.info("######################################## Number of documents" + value);
		
		
	}
}

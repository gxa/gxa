package ae3.service.search;

import org.junit.Test;

import ae3.AtlasAbstractTest;

public class AeSearchServiceTest extends AtlasAbstractTest
{
	String keywords = "cancer";
	Long arrayDesId = new Long(1608943079);
	String species = "Mus musculus";
	private boolean writeXmlToFile = true;

	@Test
	public void test_getNumberOfDoc() throws Exception
	{
		
		long value = AeSearchService.getNumberOfDoc(null, null, null);
		assertEquals(0, value);
		
		value=AeSearchService.getNumberOfDoc(keywords, null, null);
		if (value == 0)
			fail("value is 0");
		log.info("######################################## Number of documents [keywords] is " + value);
		value=AeSearchService.getNumberOfDoc(keywords, species, null);
		if (value == 0)
			fail("value is 0");
		log.info("######################################## Number of documents [keywords + species] is " + value);
		
		value=AeSearchService.getNumberOfDoc(keywords, species, arrayDesId);
		assertEquals(0, value);
		log.info("######################################## Number of documents [keywords + species + arrayDesId] is " + value);
		
		//get only arraydesid
		value=AeSearchService.getNumberOfDoc(null, null, arrayDesId);
		assertTrue("Find", value != 0);
		log.info("######################################## Number of documents [arrayDesId] is" + value);		
		
	}
	
	@Test
	public void test_searchIdxAer() throws Exception
	{
		long value = 0;		
		value=AeSearchService.getNumberOfDoc(keywords, null, null);		
		String xml=AeSearchService.searchIdxAer(keywords, null, null, 0, 1, null,null);
		log.info("##########################################################");
		//File fileXml = new File("test\\a.xml");			
		log.info(xml);
		
		value=AeSearchService.getNumberOfDoc(keywords, species, null);		
		xml=AeSearchService.searchIdxAer(keywords, species, null, 0, 1, null,null);		
		log.info("##########################################################");
		log.info(xml);
		
		value=AeSearchService.getNumberOfDoc(null, null, arrayDesId);
		xml=AeSearchService.searchIdxAer(keywords, species, arrayDesId, 0, 1, null,null);
		log.info("##########################################################");	
		log.info(xml);
		
		xml=AeSearchService.searchIdxAer("E-MEXP-444", null, null, 0, 1, null, null);
		log.info("##########################################################");	
		log.info(xml);		

	}
}

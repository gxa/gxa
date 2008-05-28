package ae3.service.search;

import java.util.Iterator;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Test;

import ae3.AtlasAbstractTest;
import ae3.service.QueryHelper;

public class AeSearchServiceTest extends AtlasAbstractTest
{
	String keywords = "cancer";
	Long arrayDesId = new Long(119901743);
	String species = "Homo sapiens";
	private boolean writeXmlToFile = true;

	@Test
	public void test_getNumOfDoc() throws Exception
	{
		
		long value = AeSearchService.getNumOfDoc(null, null, null);
		assertEquals(0, value);
		
		value=AeSearchService.getNumOfDoc(keywords, null, null);
		if (value == 0)
			fail("value is 0");
		log.info("######################################## Number of documents [keywords] is " + value);
		value=AeSearchService.getNumOfDoc(keywords, species, null);
		if (value == 0)
			fail("value is 0");
		log.info("######################################## Number of documents [keywords + species] is " + value);
		
		value=AeSearchService.getNumOfDoc(keywords, species, arrayDesId);
		//assertEquals(1, value);
		log.info("######################################## Number of documents [keywords + species + arrayDesId] is " + value);
		
		//get only arraydesid
		value=AeSearchService.getNumOfDoc(null, null, arrayDesId);
		assertTrue("Find", value != 0);
		log.info("######################################## Number of documents [arrayDesId] is" + value);		
		
	}
	
	@Test
	public void test_getNumOfDocAndFacet() throws Exception
	{
	    String query = QueryHelper.prepareQuery(null, null, null);
	    SolrDocumentList list = AeSearchService.getNumOfDocAndFacet(query);
	    assertNull(list);
	    
	    query = QueryHelper.prepareQuery(keywords, null, null);
	    list = AeSearchService.getNumOfDocAndFacet(query);
	    long count =  list.getNumFound();
	    assertEquals(AeSearchService.getNumOfDoc(query),count ); 
	    this.info("Number" + list.getNumFound());
	    Iterator<SolrDocument> it=list.iterator();
	    SolrDocument doc;
	    
	}
	
	@Test
	public void test_searchIdxAer() throws Exception
	{
		long value = 0;		
		value=AeSearchService.getNumOfDoc(keywords, null, null);		
		String xml=AeSearchService.searchIdxAer(keywords, null, null, 0, 1, null,null);
		log.info("##########################################################");
		//File fileXml = new File("test\\a.xml");			
		log.info(xml);
		
		value=AeSearchService.getNumOfDoc(keywords, species, null);		
		xml=AeSearchService.searchIdxAer(keywords, species, null, 0, 1, null,null);		
		log.info("##########################################################");
		log.info(xml);
		
		value=AeSearchService.getNumOfDoc(null, null, arrayDesId);
		xml=AeSearchService.searchIdxAer(keywords, species, arrayDesId, 0, 1, null,null);
		log.info("##########################################################");	
		log.info(xml);
		
		xml=AeSearchService.searchIdxAer("E-MEXP-444", null, null, 0, 1, null, null);
		log.info("##########################################################");	
		log.info(xml);		

	}
}

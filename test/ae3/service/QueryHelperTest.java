package ae3.service;

import ae3.AtlasAbstractTest;

public class QueryHelperTest extends AtlasAbstractTest
{
	public void test_parseQuery()
	{
		String[] keywords = {"cancer"};
		String query = QueryHelper.prepareQueryByKeywords(keywords);
		log.info("######### Query is " + query);
	}
}

package ae3.service;

public class QueryHelperTest extends AtlasAbstractTest
{
	public void test_parseQuery()
	{
		String[] keywords = {"cancer"};
		String query = QueryHelper.createQuery(keywords);
		log.info("######### Query is " + query);
	}
}

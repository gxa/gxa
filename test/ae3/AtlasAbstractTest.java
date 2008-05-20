package ae3;

import java.util.logging.Logger;

import ae3.service.ArrayExpressSearchService;

import junit.framework.TestCase;

public class AtlasAbstractTest extends TestCase
{
	protected Logger log = Logger.getLogger(getClass().getCanonicalName());

	@Override
	protected void setUp() throws Exception
	{
		log.info("Set up");
		ArrayExpressSearchService.instance().setSolrIndexLocation("D:/tools/workspaces/ebi2/ae3/indexbuilder/data/multicore");
		ArrayExpressSearchService.instance().initialize();		
		super.setUp();
		
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();		
		log.info("Shutdown");
		ArrayExpressSearchService.instance().shutdown();
	}
}

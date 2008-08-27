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
		//ArrayExpressSearchService.instance().setSolrIndexLocation("C:/Users/mdylag/workspaces/ebi/ae3/indexbuilder/data/multicore");
		ArrayExpressSearchService.instance().setSolrIndexLocation("indexbuilder/data/multicore");
		
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
	
	protected void info(String message)
	{
	    log.info("###########################################################");
	    log.info(message);
	    log.info("###########################################################");

	}
}

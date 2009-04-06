package ae3;

import java.util.logging.Logger;

import ae3.service.ArrayExpressSearchService;
import ae3.util.AtlasProperties;

import junit.framework.TestCase;

public abstract class AtlasAbstractTest extends TestCase
{
	protected Logger log = Logger.getLogger(getClass().getCanonicalName());

	@Override
	protected void setUp() throws Exception
	{
		log.info("Set up");

        ArrayExpressSearchService as = ArrayExpressSearchService.instance();
        // DBhandler dbHandler = DBhandler.instance();
        final String solrIndexLocation = AtlasProperties.getProperty("atlas.solrIndexLocation");
        final String dbName            = AtlasProperties.getProperty("atlas.dbName");
        final String netCDFlocation    = AtlasProperties.getProperty("atlas.netCDFlocation");

        log.info("Initializing Atlas...");
        log.info("  Solr index location: " + solrIndexLocation);
        log.info("  database name: " + dbName );
        log.info("  netCDF location: " + netCDFlocation );

        as.setSolrIndexLocation(solrIndexLocation);

        oracle.jdbc.pool.OracleDataSource ds = new oracle.jdbc.pool.OracleDataSource();
        ds.setURL("jdbc:oracle:thin:aemart/marte@moe:1521:AEDWDEV");

        as.setAEDataSource(ds);
        as.initialize();
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

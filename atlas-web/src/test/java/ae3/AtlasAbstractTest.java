package ae3;

import java.util.logging.Logger;

import ae3.service.ArrayExpressSearchService;
import ae3.util.AtlasProperties;

import javax.naming.Context;
import javax.sql.DataSource;

import junit.framework.TestCase;

public abstract class AtlasAbstractTest extends TestCase
{
	protected Logger log = Logger.getLogger(getClass().getCanonicalName());

	@Override
	protected void setUp() throws Exception
	{
		log.info("Set up");
		//ArrayExpressSearchService.instance().setSolrIndexLocation("C:/Users/mdylag/workspaces/ebi/ae3/indexbuilder/data/multicore");

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

	    org.h2.jdbcx.JdbcDataSource memds = new org.h2.jdbcx.JdbcDataSource();
	    String tmp = System.getProperty("java.io.tmp");
            memds.setURL("jdbc:h2:" + tmp + "/ATLAS");

	    oracle.jdbc.pool.OracleDataSource ds = new oracle.jdbc.pool.OracleDataSource();
	    ds.setURL("jdbc:oracle:thin:aemart/marte@moe:1521:AEDWDEV");

            //dbHandler.setMEMDataSource(memds);
            //dbHandler.setAEDataSource(ds);
            //DS_DBconnection.instance().setAEDataSource(ds);
            as.setMEMDataSource(memds);
            as.setAEDataSource(ds);
            as.initialize();
            // DataServerAPI.setNetCDFPath(netCDFlocation);
		
	//	ArrayExpressSearchService.instance().initialize();		
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

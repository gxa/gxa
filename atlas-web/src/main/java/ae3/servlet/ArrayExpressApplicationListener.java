package ae3.servlet;

/**
 * User: ostolop
 * Date: 07-Feb-2008
 *
 * EBI Microarray Informatics Team (c) 2007 
 */

import org.slf4j.bridge.SLF4JBridgeHandler;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ds.server.DataServerAPI;
import ds.utils.DS_DBconnection;

import ae3.service.ArrayExpressSearchService;
import ae3.service.AtlasDownloadService;
import ae3.util.AtlasProperties;

public class ArrayExpressApplicationListener implements ServletContextListener,
        HttpSessionListener, HttpSessionAttributeListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Public constructor is required by servlet spec
    public ArrayExpressApplicationListener() {
    }

    // -------------------------------------------------------
    // ServletContextListener implementation
    // -------------------------------------------------------
    public void contextInitialized(ServletContextEvent sce) {

        try {
            SLF4JBridgeHandler.install();            

            System.setProperty("java.awt.headless", "true");

            ArrayExpressSearchService as = ArrayExpressSearchService.instance();
            final String solrIndexLocation = AtlasProperties.getProperty("atlas.solrIndexLocation");
            final String dbName            = AtlasProperties.getProperty("atlas.dbName");
            final String netCDFlocation = AtlasProperties.getProperty("atlas.netCDFlocation");

            log.info("Initializing Atlas...");
            log.info("  Solr index location: " + solrIndexLocation);
            log.info("  database name: " + dbName );
            log.info("  netCDF location: " + netCDFlocation );
            log.info("  software version: " + AtlasProperties.getProperty("atlas.software.version"));
            log.info("  data release:" + AtlasProperties.getProperty("atlas.data.release"));
            log.info("  build number:" + AtlasProperties.getProperty("atlas.buildNumber"));

            as.setSolrIndexLocation(solrIndexLocation);

            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:/comp/env");
            DataSource ds = (DataSource) envContext.lookup("jdbc/" + dbName);

            DS_DBconnection.instance().setAEDataSource(ds);

            as.setAEDataSource(ds);
            as.initialize();
            
            DataServerAPI.setNetCDFPath(netCDFlocation);
        } catch (Exception e) {
            throw new RuntimeException("Error in initialization", e);
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        /* This method is invoked when the Servlet Context
           (the Web application) is undeployed or 
           Application Server shuts down.
        */

        ServletContext sc = sce.getServletContext();

        ArrayExpressSearchService.instance().shutdown();

        SLF4JBridgeHandler.uninstall();
    }

    // -------------------------------------------------------
    // HttpSessionListener implementation
    // -------------------------------------------------------
    public void sessionCreated(HttpSessionEvent se) {
        /* Session is created. */
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        ArrayExpressSearchService.instance().getDownloadService().cleanupDownloads(se.getSession().getId());
    }

    // -------------------------------------------------------
    // HttpSessionAttributeListener implementation
    // -------------------------------------------------------

    public void attributeAdded(HttpSessionBindingEvent sbe) {
        /* This method is called when an attribute 
           is added to a session.
        */
    }

    public void attributeRemoved(HttpSessionBindingEvent sbe) {
        /* This method is called when an attribute
           is removed from a session.
        */
    }

    public void attributeReplaced(HttpSessionBindingEvent sbe) {
        /* This method is invoked when an attibute
           is replaced in a session.
        */
    }
}

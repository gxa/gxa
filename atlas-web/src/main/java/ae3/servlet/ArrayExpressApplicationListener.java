package ae3.servlet; /**
 * User: ostolop
 * Date: 07-Feb-2008
 *
 * EBI Microarray Informatics Team (c) 2007 
 */

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ds.server.DataServerAPI;
import ds.utils.DS_DBconnection;

import ae3.service.ArrayExpressSearchService;
import ae3.service.AtlasResultSet;
import ae3.util.DBhandler;

import java.util.HashSet;
import java.util.Properties;

public class ArrayExpressApplicationListener implements ServletContextListener,
        HttpSessionListener, HttpSessionAttributeListener {

    private final Log log = LogFactory.getLog(getClass());

    // Public constructor is required by servlet spec
    public ArrayExpressApplicationListener() {
    }

    // -------------------------------------------------------
    // ServletContextListener implementation
    // -------------------------------------------------------
    public void contextInitialized(ServletContextEvent sce) {

        try {
            Properties atlasProps = new Properties();
            atlasProps.load(getClass().getResourceAsStream("/atlas.properties"));

//            ServletContext sc = sce.getServletContext();

            ArrayExpressSearchService as = ArrayExpressSearchService.instance();
            DBhandler dbHandler = DBhandler.instance(); 
            final String solrIndexLocation = atlasProps.getProperty("atlas.solrIndexLocation");
            final String dbName            = atlasProps.getProperty("atlas.dbName");
            final String netCDFlocation = atlasProps.getProperty("atlas.netCDFlocation");

            log.info("Initializing Atlas...");
            log.info("  Solr index location: " + solrIndexLocation);
            log.info("  database name: " + dbName );
            log.info("  netCDF location: " + netCDFlocation );

            as.setSolrIndexLocation(solrIndexLocation);

            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:/comp/env");
            DataSource ds = (DataSource) envContext.lookup("jdbc/" + dbName);
            DataSource memds = (DataSource) envContext.lookup("jdbc/ATLAS");

            dbHandler.setMEMDataSource(memds);
            dbHandler.setAEDataSource(ds);
            DS_DBconnection.instance().setAEDataSource(ds);
            as.setMEMDataSource(memds);
            as.setAEDataSource(ds);
            as.initialize();
            DataServerAPI.setNetCDFPath(netCDFlocation);

        } catch (Exception e) {
            log.error(e);
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        /* This method is invoked when the Servlet Context
           (the Web application) is undeployed or 
           Application Server shuts down.
        */

        ServletContext sc = sce.getServletContext();

        ArrayExpressSearchService as = ArrayExpressSearchService.instance();
        as.shutdown();

        LogFactory.releaseAll();
    }

    // -------------------------------------------------------
    // HttpSessionListener implementation
    // -------------------------------------------------------
    public void sessionCreated(HttpSessionEvent se) {
        /* Session is created. */
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        /* Sesssion is destroyed */
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

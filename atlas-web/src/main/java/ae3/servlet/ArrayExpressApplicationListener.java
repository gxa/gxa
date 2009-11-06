package ae3.servlet;

/**
 * User: ostolop
 * Date: 07-Feb-2008
 *
 * EBI Microarray Informatics Team (c) 2007 
 */

import ae3.service.AtlasSearchService;
import ae3.util.AtlasProperties;
import ds.server.DataServerAPI;
import ds.utils.DS_DBconnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

public class ArrayExpressApplicationListener implements ServletContextListener,
        HttpSessionListener, HttpSessionAttributeListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public void contextInitialized(ServletContextEvent sce) {
        // setup SLF4J bridge, in case any dependencies use other logging solutions
        SLF4JBridgeHandler.install();

        // get context, driven by config
        ServletContext application = sce.getServletContext();
        WebApplicationContext context =
                WebApplicationContextUtils.getWebApplicationContext(application);

        // acquire the beans we need
        File atlasIndex = (File) context.getBean("atlasIndex");
        File atlasNetCDFRepo = (File) context.getBean("atlasNetCDFRepo");
        AtlasSearchService as = (AtlasSearchService) context.getBean("atlasSearchService");
        DataSource atlasDataSource = (DataSource) context.getBean("atlasDataSource");

        // read out the URL from the database
        String atlasDatasourceUrl;
        try {
            Connection c = atlasDataSource.getConnection();
            DatabaseMetaData dmd = c.getMetaData();
            atlasDatasourceUrl = dmd.getURL();
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new NullPointerException("Unable to obtain connection to the datasource, or failed to read URL");
        }

        // read version file
        Properties versionProps = new Properties();
        try {
            versionProps.load(getClass().getClassLoader().getResourceAsStream("META-INF/atlas.version"));
        } catch (IOException e) {
            throw new NullPointerException("Cannot load atlas version properties - " +
                    "META-INF/atlas.version may be missing or invalid");
        }
        if (versionProps.getProperty("atlas.buildNumber") == null ||
                versionProps.getProperty("atlas.software.version") == null) {
            throw new NullPointerException("Cannot load atlas version properties - " +
                    "META-INF/atlas.version may be missing or invalid");
        }

        log.info("Atlas initialized with the following parameters...");
        // software properties
        log.info("\tBuild Number:               " + versionProps.getProperty("atlas.buildNumber"));
        log.info("\tSoftware Version:           " + versionProps.getProperty("atlas.software.version"));
        // data properties
        log.info("\tData Release:               " + AtlasProperties.getProperty("atlas.data.release")); // fixme: read this from DB
        // context properties
        log.info("\tSOLR Index Location:        " + atlasIndex);
        log.info("\tAtlas DataSource:           " + atlasDatasourceUrl);
        log.info("\tNetCDF repository Location: " + atlasNetCDFRepo.getAbsolutePath());

        // last bits of setup for DS_DBconnection
        DS_DBconnection.instance().setAEDataSource(atlasDataSource);

        as.setAtlasDataSource(atlasDataSource);
        as.initialize();

        DataServerAPI.setNetCDFPath(atlasNetCDFRepo.getAbsolutePath());
    }

    public void contextDestroyed(ServletContextEvent sce) {
        /* This method is invoked when the Servlet Context
           (the Web application) is undeployed or
           Application Server shuts down.
        */

        ServletContext sc = sce.getServletContext();

        AtlasSearchService.instance().shutdown();

        SLF4JBridgeHandler.uninstall();
    }

    // -------------------------------------------------------
    // HttpSessionListener implementation
    // -------------------------------------------------------
    public void sessionCreated(HttpSessionEvent se) {
        /* Session is created. */
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        AtlasSearchService.instance().getDownloadService()
                .cleanupDownloads(se.getSession().getId());
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

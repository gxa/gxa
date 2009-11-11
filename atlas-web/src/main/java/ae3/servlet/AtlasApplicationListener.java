package ae3.servlet;

/**
 * User: ostolop
 * Date: 07-Feb-2008
 *
 * EBI Microarray Informatics Team (c) 2007 
 */

import ae3.service.AtlasPlotter;
import ae3.util.AtlasProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.ac.ebi.gxa.web.Atlas;
import uk.ac.ebi.gxa.web.AtlasSearchService;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

public class AtlasApplicationListener implements ServletContextListener, HttpSessionListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public void contextInitialized(ServletContextEvent sce) {
        // setup SLF4J bridge, in case any dependencies use other logging solutions
        SLF4JBridgeHandler.install();

        // get context, driven by config
        ServletContext application = sce.getServletContext();
        WebApplicationContext context =
                WebApplicationContextUtils.getWebApplicationContext(application);

        // initialize and store in-session the AtlasSearchService
        AtlasSearchService searchService = (AtlasSearchService) context.getBean("atlasSearchService");
        application.setAttribute(Atlas.SEARCH_SERVICE.key(), searchService);

        // initialize and store in-session the AtlasPLotter
        AtlasPlotter plotter = (AtlasPlotter) context.getBean("atlasPlotter");
        application.setAttribute(Atlas.PLOTTER.key(), plotter);

        // discover our datasource URL from the database metadata
        DataSource atlasDataSource = (DataSource) context.getBean("atlasDataSource");
        String atlasDatasourceUrl;
        try {
            Connection c = atlasDataSource.getConnection();
            DatabaseMetaData dmd = c.getMetaData();
            System.out.println("Got metadata ok!");
            atlasDatasourceUrl = dmd.getURL();
            c.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new NullPointerException("Unable to obtain connection to the datasource, or failed to read URL");
        }

        // read versioning info
        Properties versionProps = new Properties();
        try {
            versionProps.load(getClass().getClassLoader().getResourceAsStream("atlas.version"));
        }
        catch (IOException e) {
            throw new NullPointerException("Cannot load atlas version properties - " +
                    "META-INF/atlas.version may be missing or invalid");
        }
        if (versionProps.getProperty("atlas.buildNumber") == null ||
                versionProps.getProperty("atlas.software.version") == null) {
            throw new NullPointerException("Cannot load atlas version properties - " +
                    "META-INF/atlas.version may be missing or invalid");
        }

        // read index, netcdf directory locations
        String atlasIndex = ((File) context.getBean("atlasIndex")).getAbsolutePath();
        String atlasNetCDFRepo = ((File) context.getBean("atlasNetCDFRepo")).getAbsolutePath();

        log.info("Atlas initialized with the following parameters...");
        // software properties
        log.info("\tBuild Number:               " + versionProps.getProperty("atlas.buildNumber"));
        log.info("\tSoftware Version:           " + versionProps.getProperty("atlas.software.version"));
        // data properties
        log.info("\tData Release:               " +
                AtlasProperties.getProperty("atlas.data.release")); // fixme: read this from DB
        // context properties
        log.info("\tSOLR Index Location:        " + atlasIndex);
        log.info("\tAtlas DataSource:           " + atlasDatasourceUrl);
        log.info("\tNetCDF repository Location: " + atlasNetCDFRepo);
    }

    public void contextDestroyed(ServletContextEvent sce) {
        // remove slf4j bridge
        SLF4JBridgeHandler.uninstall();

        // get context, driven by config
        ServletContext application = sce.getServletContext();
        WebApplicationContext context =
                WebApplicationContextUtils.getWebApplicationContext(application);

        // shutdown and remove from session the AtlasSearchService
        AtlasSearchService searchService = (AtlasSearchService) context.getBean("atlasSearchService");
        searchService.shutdown();
        application.removeAttribute(Atlas.SEARCH_SERVICE.key());

    }

    public void sessionCreated(HttpSessionEvent se) {
        // do nothing
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        ServletContext application = se.getSession().getServletContext();
        WebApplicationContext context =
                WebApplicationContextUtils.getWebApplicationContext(application);

        AtlasSearchService searchService = (AtlasSearchService) context.getBean("atlasSearchService");
        searchService.getAtlasDownloadService().cleanupDownloads(se.getSession().getId());
    }
}

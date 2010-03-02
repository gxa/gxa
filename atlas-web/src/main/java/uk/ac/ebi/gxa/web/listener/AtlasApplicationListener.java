/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.web.listener;

import ae3.dao.AtlasDao;
import ae3.service.AtlasDownloadService;
import ae3.service.GeneListCacheService;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.util.AtlasProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.ac.ebi.gxa.R.AtlasRServicesException;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.web.Atlas;
import uk.ac.ebi.gxa.web.AtlasPlotter;
import uk.ac.ebi.microarray.atlas.model.AtlasStatistics;

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
import java.util.logging.LogManager;

/**
 * A {@link ServletContextListener} for the Atlas web application.  To use the atlas codebase, a listener should be
 * registered in the applications web.xml that invoks this listener at startup.  This listener will configure and store
 * in session any services required by the atlas web interface.
 *
 * @author Misha Kapushesky
 * @author Tony Burdett
 * @date 07-Feb-2008 EBI Microarray Informatics Team (c) 2007
 */
public class AtlasApplicationListener implements ServletContextListener, HttpSessionListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public void contextInitialized(ServletContextEvent sce) {
        long start = System.currentTimeMillis();
        log.info("Starting up atlas");

        // use SLF4J to configure logging
        SLF4JBridgeHandler.install();

        // get context, driven by config
        ServletContext application = sce.getServletContext();
        WebApplicationContext context =
                WebApplicationContextUtils.getWebApplicationContext(application);

        // fetch services from the context
        AtlasDAO atlasDAO = (AtlasDAO) context.getBean("atlasInterfaceDAO");
        AtlasDownloadService downloadService = (AtlasDownloadService) context.getBean("atlasDownloadService");
        AtlasComputeService computeService = (AtlasComputeService) context.getBean("atlasComputeService");
        AtlasStructuredQueryService queryService = (AtlasStructuredQueryService) context.getBean("atlasQueryService");
        AtlasDao atlasSolrDAO = (AtlasDao) context.getBean("atlasSolrDAO");
        GeneListCacheService geneCacheService = (GeneListCacheService) context.getBean("geneListCacheService");

        // store in session
        application.setAttribute(Atlas.ATLAS_DAO.key(), atlasDAO);
        application.setAttribute(Atlas.DOWNLOAD_SERVICE.key(), downloadService);
        application.setAttribute(Atlas.ATLAS_SOLR_DAO.key(), atlasSolrDAO);
        application.setAttribute(Atlas.GENES_CACHE.key(), geneCacheService);

        String dataRelease = AtlasProperties.getProperty("atlas.data.release");
        AtlasStatistics statistics = atlasDAO.getAtlasStatisticsByDataRelease(dataRelease);
        application.setAttribute("atlasStatistics", statistics);
        application.setAttribute("atlasQueryService", queryService);

        try {
            // check that the AtlasRFactory associated with our search service is actually working
            // fixme: serious UnsatisfiedLinkError problem [no jri in java.library.path]...  
            // doing this on a LocalFactory (which calls DirectJNI.getInstance() to check) can cause a fatal error
            // that will bring down tomcat if R environment is not configured correctly, but variables are set
            if (!computeService.getAtlasRFactory().validateEnvironment()) {
                log.warn("R computation environment not valid/present.  Atlas on-the-fly computations will fail");
            }
            else {
                log.info("R environment validated, R services fully available");
            }
        }
        catch (AtlasRServicesException e) {
            e.printStackTrace();
            throw new RuntimeException("R computation environment not valid/present: " + e.getMessage());
        }

        // discover our datasource URL from the database metadata
        DataSource atlasDataSource = (DataSource) context.getBean("atlasDataSource");
        String atlasDatasourceUrl, atlasDatasourceUser;
        try {
            Connection c = DataSourceUtils.getConnection(atlasDataSource);
            DatabaseMetaData dmd = c.getMetaData();
            atlasDatasourceUrl = dmd.getURL();
            atlasDatasourceUser = dmd.getUserName();
            DataSourceUtils.releaseConnection(c, atlasDataSource);
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to obtain connection to the datasource, or failed to read URL");
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

        StringBuffer sb = new StringBuffer();
        sb.append("\nAtlas initializing with the following parameters...");
        // software properties
        sb.append("\n\tBuild Number:               ").append(versionProps.getProperty("atlas.buildNumber"));
        sb.append("\n\tSoftware Version:           ").append(versionProps.getProperty("atlas.software.version"));
        // data properties
        // fixme: read this from DB
        sb.append("\n\tData Release:               ").append(AtlasProperties.getProperty("atlas.data.release"));
        // context properties
        sb.append("\n\tSOLR Index Location:        ").append(atlasIndex);
        sb.append("\n\tAtlas DataSource:           ").append(atlasDatasourceUrl)
                .append(" (user ").append(atlasDatasourceUser).append(")");
        sb.append("\n\tNetCDF repository Location: ").append(atlasNetCDFRepo);
        log.info(sb.toString());

        long end = System.currentTimeMillis();
        double time = ((double) (end - start)) / 1000;

        log.info("Atlas startup completed in " + time + " s.");
    }

    public void contextDestroyed(ServletContextEvent sce) {
        // remove slf4j bridge
        SLF4JBridgeHandler.uninstall();

        log.info("Shutting down atlas...");
        long start = System.currentTimeMillis();

        // get context, driven by config
        ServletContext application = sce.getServletContext();

        // shutdown and remove services from session
        application.removeAttribute(Atlas.ATLAS_SOLR_DAO.key());
        application.removeAttribute(Atlas.ATLAS_DAO.key());

        application.removeAttribute(Atlas.DOWNLOAD_SERVICE.key());
        application.removeAttribute(Atlas.GENES_CACHE.key());

        long end = System.currentTimeMillis();
        double time = ((double) end - start) / 1000;
        log.info("Atlas shutdown complete in " + time + " s.");
    }

    public void sessionCreated(HttpSessionEvent se) {
        // do nothing
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        ServletContext application = se.getSession().getServletContext();

        // cleanup any downloads being done in this session
        AtlasDownloadService downloadService =
                (AtlasDownloadService) application.getAttribute(Atlas.DOWNLOAD_SERVICE.key());
        downloadService.cleanupDownloads(se.getSession().getId());
    }
}

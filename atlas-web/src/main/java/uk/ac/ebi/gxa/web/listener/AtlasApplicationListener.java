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

import ae3.dao.GeneSolrDAO;
import ae3.service.AtlasDownloadService;
import ae3.service.GeneListCacheService;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.ManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ucar.nc2.dataset.NetcdfDataset;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRServicesException;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.AtlasPropertiesListener;
import uk.ac.ebi.gxa.web.Atlas;
import uk.ac.ebi.microarray.atlas.model.AtlasStatistics;

import javax.management.MBeanServer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;

/**
 * A {@link ServletContextListener} for the Atlas web application.  To use the atlas codebase, a listener should be
 * registered in the applications web.xml that invoks this listener at startup.  This listener will configure and store
 * in session any services required by the atlas web interface.
 *
 * @author Misha Kapushesky
 * @author Tony Burdett
 */
public class AtlasApplicationListener implements ServletContextListener, HttpSessionListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public void contextInitialized(ServletContextEvent sce) {
        long start = System.currentTimeMillis();
        log.info("Starting up atlas");

        // use SLF4J to configure logging
        SLF4JBridgeHandler.install();

        // get context, driven by config
        final ServletContext application = sce.getServletContext();
        WebApplicationContext context;
        try {
            context =
                    WebApplicationContextUtils.getWebApplicationContext(application);
        } catch (Throwable e) {
            log.error("\n\n**** FATAL STARTUP ERROR ****\nFailed to get web application context! Is your context set up correctly?\n\n");
            return;
        }

        // fetch services from the context
        final AtlasDAO atlasDAO = context.getBean(AtlasDAO.class);
        AtlasStructuredQueryService queryService = context.getBean(AtlasStructuredQueryService.class);
        GeneSolrDAO geneSolrDAO = context.getBean(GeneSolrDAO.class);
        GeneListCacheService geneCacheService = context.getBean(GeneListCacheService.class);
        final AtlasProperties atlasProperties = context.getBean(AtlasProperties.class);

        // store in session
        application.setAttribute(Atlas.ATLAS_DAO.key(), atlasDAO);
        application.setAttribute(Atlas.ATLAS_SOLR_DAO.key(), geneSolrDAO);
        application.setAttribute(Atlas.GENES_CACHE.key(), geneCacheService);

        atlasProperties.registerListener(new AtlasPropertiesListener() {
            String lastDate = atlasProperties.getLastReleaseDate();

            public void onAtlasPropertiesUpdate(AtlasProperties atlasProperties) {
                String releaseDate = atlasProperties.getLastReleaseDate();
                if (releaseDate.equals(lastDate))
                    return;
                lastDate = releaseDate;
                updateStatistics(atlasProperties, atlasDAO, application);
            }
        });

        updateStatistics(atlasProperties, atlasDAO, application);

        application.setAttribute("atlasQueryService", queryService);
        application.setAttribute("atlasProperties", atlasProperties);

        // check that the AtlasRFactory associated with our search service is actually working
        // fixme: serious UnsatisfiedLinkError problem [no jri in java.library.path]...
        // doing this on a LocalFactory (which calls DirectJNI.getInstance() to check) can cause a fatal error
        // that will bring down tomcat if R environment is not configured correctly, but variables are set
        AtlasRFactory rFactory = context.getBean(AtlasRFactory.class);
        try {
            if (!rFactory.validateEnvironment()) {
                log.warn("R computation environment not valid/present.  Atlas on-the-fly computations will fail");
            } else {
                log.info("R environment validated, R services fully available");
            }
        } catch (AtlasRServicesException e) {
            log.error("R environment validation failed.  Atlas on-the-fly computations will fail", e);
        } catch (UnsatisfiedLinkError ule) {
            log.error("Atlas configured to use local R which is not present. Atlas on-the-fly computations will fail", ule);
        }

        // discover our datasource URL from the database metadata
        DataSource atlasDataSource = context.getBean(DataSource.class);
        String atlasDatasourceUrl, atlasDatasourceUser;
        try {
            Connection c = DataSourceUtils.getConnection(atlasDataSource);
            DatabaseMetaData dmd = c.getMetaData();
            atlasDatasourceUrl = dmd.getURL();
            atlasDatasourceUser = dmd.getUserName();
            DataSourceUtils.releaseConnection(c, atlasDataSource);
        } catch (SQLException e) {
            throw logUnexpected("Unable to obtain connection to the datasource, or failed to read URL", e);
        }

        // read versioning info

        // read index, netcdf directory locations
        String atlasIndex = context.getBean("atlasIndex", File.class).getAbsolutePath();
        String atlasDataRepo = context.getBean("atlasDataRepo", File.class).getAbsolutePath();

        NetcdfDataset.initNetcdfFileCache(0, 60, 30);

        StringBuffer sb = new StringBuffer();
        sb.append("\nAtlas initializing with the following parameters...");
        // software properties
        sb.append("\n\tSoftware Version:           ").append(atlasProperties.getSoftwareVersion());
        sb.append("\n\tBuilt on:                   ").append(atlasProperties.getSoftwareDate());
        // data properties
        // fixme: read this from DB
        sb.append("\n\tData Release:               ").append(atlasProperties.getDataRelease());
        // context properties
        sb.append("\n\tSOLR Index Location:        ").append(atlasIndex);
        sb.append("\n\tAtlas DataSource:           ").append(atlasDatasourceUrl)
                .append(" (user ").append(atlasDatasourceUser).append(")");
        sb.append("\n\tData repository Location: ").append(atlasDataRepo);
        log.info(sb.toString());

        long end = System.currentTimeMillis();
        double time = ((double) (end - start)) / 1000;

        log.info("Atlas startup completed in " + time + " s.");

        CacheManager manager = CacheManager.getInstance();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ManagementService.registerMBeans(manager, mBeanServer, false, false, false, true);
    }

    private void updateStatistics(AtlasProperties atlasProperties, AtlasDAO atlasDAO, ServletContext application) {
        AtlasStatistics statistics = atlasDAO.getAtlasStatistics(
                atlasProperties.getDataRelease(),
                atlasProperties.getLastReleaseDate());
        application.setAttribute("atlasStatistics", statistics);
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

        application.removeAttribute(Atlas.GENES_CACHE.key());

        NetcdfDataset.shutdown();

        CacheManager.getInstance().shutdown();

        long end = System.currentTimeMillis();
        double time = ((double) end - start) / 1000;
        log.info("Atlas shutdown complete in " + time + " s.");
    }

    public void sessionCreated(HttpSessionEvent se) {
        // do nothing
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        ServletContext application = se.getSession().getServletContext();
        WebApplicationContext context =
                WebApplicationContextUtils.getWebApplicationContext(application);
        AtlasDownloadService downloadService = context.getBean(AtlasDownloadService.class);
        downloadService.cleanupDownloads(se.getSession().getId());
    }
}

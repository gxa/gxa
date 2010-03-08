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

package uk.ac.ebi.gxa.jmx;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.web.context.ServletContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.sql.DataSource;
import javax.servlet.ServletContext;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.LogManager;


/**
 * @author pashky
 */
public class AtlasManager implements AtlasManagerMBean, ServletContextAware {
    final private Logger log = LoggerFactory.getLogger(getClass());

    private IndexBuilder indexBuilder;
    private File atlasIndex;
    private File netCDFRepo;
    private DataSource dataSource;
    private Efo efo;
    private ServletContext servletContext;
    private AtlasProperties atlasProperties;

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
    }

    public void setAtlasIndex(File atlasIndex) {
        this.atlasIndex = atlasIndex;
    }

    public void setNetCDFRepo(File netCDFRepo) {
        this.netCDFRepo = netCDFRepo;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    private void fixLog() {
        try {
            LogManager.getLogManager().readConfiguration(getClass().getClassLoader().getResourceAsStream("logging.properties"));
        }
        catch (Exception e) {
            //
        }
        SLF4JBridgeHandler.install();
    }

    public void rebuildIndex(String index) {
        fixLog();
        log.info("JMX: Rebuilding index " + index);
        indexBuilder.setIncludeIndexes(Collections.singletonList(index));
        indexBuilder.buildIndex();
    }

    public void rebuildAllIndexes() {
        fixLog();
        log.info("JMX: Rebuilding all indexes");
        indexBuilder.setIncludeIndexes(Arrays.asList("properties", "experiments", "genes"));
        indexBuilder.buildIndex();
    }

    public String getVersion() {
        return atlasProperties.getSoftwareVersion() + " " + atlasProperties.getSoftwareBuildNumber();
    }

    public String getIndexPath() {
        return atlasIndex.getAbsolutePath();
    }

    public String getNetCDFPath() {
        return netCDFRepo.getAbsolutePath();
    }

    public String getDataSourceURL() {
        String result = "";
        try {
            Connection c = DataSourceUtils.getConnection(dataSource);
            DatabaseMetaData dmd = c.getMetaData();
            result = dmd.getUserName() + " @ " + dmd.getURL();
            DataSourceUtils.releaseConnection(c, dataSource);
        }
        catch (SQLException e) {
            throw new RuntimeException("Unable to obtain connection to the datasource, or failed to read URL");
        }
        return result;
    }

    public String getEFO() {
        return "EFO version " + efo.getVersion() + " (" + efo.getVersionInfo() + ") loaded from " + efo.getUri().toString();
    }

    public String getAtlasProperty(String property) {
        return atlasProperties.getProperty(property);
    }

    public void setAtlasProperty(String property, String newValue) {
        log.info("JMX: Setting property " + property + " to " + newValue);
        atlasProperties.setProperty(property, newValue);
    }

    public String getWebappPath() {
        return servletContext.getRealPath("");
    }
}

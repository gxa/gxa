package uk.ac.ebi.gxa.jmx;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.web.context.ServletContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.efo.Efo;

import javax.sql.DataSource;
import javax.servlet.ServletContext;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import ae3.util.AtlasProperties;

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

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void rebuildIndex(String index) {
        log.info("JMX: Rebuilding index " + index);
        indexBuilder.setIncludeIndexes(Collections.singletonList(index));
        indexBuilder.buildIndex();
    }

    public void rebuildAllIndexes() {
        log.info("JMX: Rebuilding all indexes");
        indexBuilder.setIncludeIndexes(Arrays.asList("properties", "experiments", "genes"));
        indexBuilder.buildIndex();
    }

    public String getVersion() {
        try {
            Properties versionProps = new Properties();
            versionProps.load(getClass().getClassLoader().getResourceAsStream("atlas.version"));
            return versionProps.getProperty("atlas.software.version") + " " + versionProps.getProperty("atlas.buildNumber");
        }
        catch (Exception e) {
            return "unknown";
        }
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
            result = dmd.getURL();
            DataSourceUtils.releaseConnection(c, dataSource);
        }
        catch (SQLException e) {
            throw new RuntimeException("Unable to obtain connection to the datasource, or failed to read URL");
        }
        return result;
    }

    public String getEFO() {
        return efo.getUri().toString();
    }

    public String getAtlasProperty(String property) {
        return AtlasProperties.getProperty(property);
    }

    public void setAtlasProperty(String property, String newValue) {
        log.info("JMX: Setting property " + property + " to " + newValue);
        AtlasProperties.setProperty(property, newValue);
    }

    public String getWebappPath() {
        return servletContext.getRealPath("");
    }
}

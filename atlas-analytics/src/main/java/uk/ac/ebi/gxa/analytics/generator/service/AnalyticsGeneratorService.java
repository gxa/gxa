package uk.ac.ebi.gxa.analytics.generator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;

import java.io.InputStream;
import java.util.Properties;

/**
 * An abstract AnalyticsGeneratorService, that provides convenience methods for getting and setting parameters required
 * across all AnalyticsGenerator implementations.  This class is typed by the type of the repository backing this
 * AnalyticsGeneratorService - this may be a file, a datasource, an FTP directory, or something else. Implementing
 * classes have access to this repository and an {@link uk.ac.ebi.microarray.atlas.dao.AtlasDAO} that provides
 * interaction with the Atlas database (following an Atlas 2 schema).
 * <p/>
 * All implementing classes should provide the method {@link #createAnalytics()} which contains the logic for
 * constructing the relevant parts of the index for each implementation.  Clients should call {@link
 * #generateAnalytics()} to trigger Analytics construction.  At the moment, this method simply delegates to the abstract
 * form, but extra initialisation may go in this method.
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public abstract class AnalyticsGeneratorService<T> {
    private AtlasDAO atlasDAO;
    private T repositoryLocation;

    private AtlasComputeService atlasComputeService;

    private boolean updateMode = false;
    private boolean pendingOnly = false;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    protected String versionDescriptor;

    public AnalyticsGeneratorService(AtlasDAO atlasDAO, T repositoryLocation, AtlasComputeService atlasComputeService) {
        this.atlasDAO = atlasDAO;
        this.repositoryLocation = repositoryLocation;
        this.atlasComputeService = atlasComputeService;
    }

    public boolean getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(boolean updateMode) {
        this.updateMode = updateMode;
    }

    public boolean getPendingOnly() {
        return pendingOnly;
    }

    public void setPendingOnly(boolean pendingExps) {
        this.pendingOnly = pendingExps;
    }

    protected Logger getLog() {
        return log;
    }

    protected AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    protected T getRepositoryLocation() {
        return repositoryLocation;
    }

    protected AtlasComputeService getAtlasComputeService() {
        return atlasComputeService;
    }

    public void generateAnalytics() throws AnalyticsGeneratorException {
        versionDescriptor = lookupVersionFromMavenProperties();
        createAnalytics();
    }

    public void generateAnalyticsForExperiment(String experimentAccession)
            throws AnalyticsGeneratorException {
        versionDescriptor = lookupVersionFromMavenProperties();
        createAnalyticsForExperiment(experimentAccession);
    }

    protected abstract void createAnalytics() throws AnalyticsGeneratorException;

    protected abstract void createAnalyticsForExperiment(String experimentAccession) throws AnalyticsGeneratorException;

    private String lookupVersionFromMavenProperties() {
        String version = "Atlas Analytics Generator Version ";
        try {
            Properties properties = new Properties();
            InputStream in = getClass().getClassLoader()
                    .getResourceAsStream("META-INF/maven/uk.ac.ebi.microarray.atlas/atlas-analytics/pom.properties");
            properties.load(in);

            version = version + properties.getProperty("version");
        }
        catch (Exception e) {
            getLog().warn(
                    "Version number couldn't be discovered from pom.properties");
            version = version + "[Unknown]";
        }

        return version;
    }
}

package uk.ac.ebi.microarray.atlas.netcdf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException;

import java.io.InputStream;
import java.util.Properties;

/**
 * An abstract NetCDFGeneratorService, that provides convenience methods for
 * getting and setting parameters required across all NetCDFGenerator
 * implementations.  This class is typed by the type of the repository backing
 * this NetCDFGeneratorService - this may be a file, a datasource, an FTP
 * directory, or something else. Implementing classes have access to this
 * repository and an {@link uk.ac.ebi.microarray.atlas.dao.AtlasDAO} that
 * provides interaction with the Atlas database (following an Atlas 2 schema).
 * <p/>
 * All implementing classes should implement the method {@link
 * #createNetCDFDocs()} ()} which contains the logic for constructing the
 * relevant parts of the index for each implementation.  Clients should call
 * {@link #generateNetCDFs()} to trigger NetCDF construction.  At the moment,
 * this method simply delegates to the abstract form, but extra initialisation
 * may go in this method.
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public abstract class NetCDFGeneratorService<T> {
  private AtlasDAO atlasDAO;
  private T repositoryLocation;

  private boolean updateMode = false;
  private boolean pendingOnly = false;

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  protected String versionDescriptor;

  public NetCDFGeneratorService(AtlasDAO atlasDAO, T repositoryLocation) {
    this.atlasDAO = atlasDAO;
    this.repositoryLocation = repositoryLocation;
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

  public void generateNetCDFs() throws NetCDFGeneratorException {
    versionDescriptor = lookupVersionFromMavenProperties();
    createNetCDFDocs();
  }

  public void generateNetCDFsForExperiment(String experimentAccession)
      throws NetCDFGeneratorException {
    versionDescriptor = lookupVersionFromMavenProperties();
    createNetCDFDocsForExperiment(experimentAccession);
  }

  protected abstract void createNetCDFDocs() throws NetCDFGeneratorException;

  protected abstract void createNetCDFDocsForExperiment(
      String experimentAccession) throws NetCDFGeneratorException;

  private String lookupVersionFromMavenProperties() {
    String version = "Atlas NetCDF Generator Version ";
    try {
      Properties properties = new Properties();
      InputStream in = getClass().getClassLoader().
          getResourceAsStream("META-INF/maven/uk.ac.ebi.microarray.atlas/" +
              "netcdf-generator/pom.properties");
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

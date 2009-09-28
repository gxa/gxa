package uk.ac.ebi.microarray.atlas.netcdf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException;

/**
 * Javadocs go here!
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

  }

  protected abstract void createNetCDFDocs() throws NetCDFGeneratorException;
}

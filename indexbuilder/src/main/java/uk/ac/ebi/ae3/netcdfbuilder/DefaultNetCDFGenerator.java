package uk.ac.ebi.ae3.netcdfbuilder;

import javax.sql.DataSource;
import java.io.File;

/**
 * A default implementation of {@link NetCDFGenerator} that builds a NetCDF
 * repository at a given {@link File} on the local filesystem.
 *
 * @author Tony Burdett
 * @date 17-Sep-2009
 */
public class DefaultNetCDFGenerator implements NetCDFGenerator<File> {
  private DataSource dataSource;
  private File repositoryLocation;

  public void setAtlasDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public DataSource getAtlasDataSource() {
    return dataSource;
  }

  public void setRepositoryLocation(File repositoryLocation) {
    this.repositoryLocation = repositoryLocation;
  }

  public File getRepositoryLocation() {
    return repositoryLocation;
  }

  public void generateNetCDFs() {
    // todo - run the generator with argument "all"
  }

  public void generateNetCDFsForExperiment(String experimentAccession) {
    // todo - run the generator with argument "experimentAccession"
  }
}

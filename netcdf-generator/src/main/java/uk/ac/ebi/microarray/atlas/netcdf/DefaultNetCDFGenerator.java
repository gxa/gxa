package uk.ac.ebi.microarray.atlas.netcdf;

import org.springframework.jdbc.core.JdbcTemplate;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.netcdf.listener.NetCDFGeneratorListener;

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

  public void startup() throws NetCDFGeneratorException {
    // do some initialization...

    // create a spring jdbc template
    JdbcTemplate template = new JdbcTemplate(dataSource);

    // create an atlas dao
    AtlasDAO dao = new AtlasDAO();
    dao.setJdbcTemplate(template);
  }

  public void shutdown() throws NetCDFGeneratorException {
    // todo - really nothing to shutdown?
  }

  public void generateNetCDFs() {
    generateNetCDFs(null);
  }

  public void generateNetCDFs(NetCDFGeneratorListener listener) {
    // todo - run the generator with argument "all"
  }

  public void generateNetCDFsForExperiment(String experimentAccession) {
    generateNetCDFsForExperiment(experimentAccession, null);
  }

  public void generateNetCDFsForExperiment(String experimentAccession,
                                           NetCDFGeneratorListener listener) {
    // todo - run the generator with argument "experimentAccession"
  }
}

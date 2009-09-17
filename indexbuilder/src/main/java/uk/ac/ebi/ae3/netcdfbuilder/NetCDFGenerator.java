package uk.ac.ebi.ae3.netcdfbuilder;

import javax.sql.DataSource;

/**
 * Interface for building a NetCDFs, as required by the Atlas expression
 * analytics.  Implementations should provide a way of setting the NetCDF
 * repository location, which may be of a generic type to allow the NetCDFs to
 * be built into a repository backed by a database, file system, or some other
 * datasource.  Any implementation should implement {@link #generateNetCDFs()}
 * which contains the logic to construct the NetCDF repository.
 *
 * @author Tony Burdett
 * @date 17-Sep-2009
 */
public interface NetCDFGenerator<T> {
  /**
   * Set the location of the Atlas {@link javax.sql.DataSource} that will be
   * used to obtain the data to generate NetCDFs.  This datasource should obey
   * the atlas 2 database schema.
   *
   * @param datasource the atlas 2 compliant datasource to generate NetCDFs
   *                   from
   */
  void setAtlasDataSource(DataSource datasource);

  /**
   * Get the location of the Atlas {@link javax.sql.DataSource} that will be
   * used to obtain the data to generate NetCDFs.
   *
   * @return the Atlas 2 compliant datasource to generate NetCDFs from
   */
  DataSource getAtlasDataSource();

  /**
   * Set the location for the repository.  If there is already a pre-existing
   * repository at this location, implementations should update it.  If there is
   * no repository pre-existing, it should be created.
   *
   * @param repositoryLocation the location of the repository
   */
  void setRepositoryLocation(T repositoryLocation);

  /**
   * Get the location of the repository.  This may not exist, if the NetCDF
   * generator has not yet been run.
   *
   * @return the location of the repository
   */
  T getRepositoryLocation();

  /**
   * Run the NetCDFGenerator as a standalone, complete mechanism to generate all
   * possible NetCDFs.  If you wish to only regenerate a small slice of data,
   * use {@link #generateNetCDFsForExperiment(String)}
   */
  void generateNetCDFs();

  /**
   * Run the NetCDFGenerator to regenerate NetCDFs for only one particular
   * accession.
   *
   * @param experimentAccession the accession of the experiment to generate
   */
  void generateNetCDFsForExperiment(String experimentAccession);
}

package uk.ac.ebi.ae3.indexbuilder;

import javax.sql.DataSource;

/**
 * Interface for building a Gene Expression Atlas index.  Implementations should
 * provide a way of setting the index location, which may be of a generic type
 * to allow the index to be backed by a database, file system, or some other
 * storage medium. IndexBuilder implementations should implement {@link
 * #buildIndex()} which contains the logic to construct the index.
 * <p/>
 * By default, all genes and experiments are included, and all experiments (both
 * pending and non-pending) are included.
 *
 * @author Tony Burdett
 * @date 20-Aug-2009
 */
public interface IndexBuilder<T> {
  /**
   * Set the location of the Atlas {@link javax.sql.DataSource} that will be
   * used to obtain the data to build the index.  This datasource should obey
   * the atlas 2 database schema.
   *
   * @param datasource the atlas 2 compliant datasource to build the index from
   */
  void setAtlasDataSource(DataSource datasource);

  /**
   * Get the location of the Atlas {@link javax.sql.DataSource} that will be
   * used to obtain the data to build the index.
   *
   * @return the Atlas 2 compliant datasource to build the index from
   */
  DataSource getAtlasDataSource();

  /**
   * Set the location for the index.  If there is already a pre-existing index
   * at this location, implementations should update this index.  If there is no
   * index pre-existing, it should be created.
   *
   * @param indexLocation the location of the index
   */
  void setIndexLocation(T indexLocation);

  /**
   * Get the location of the index.  This may not exist, if the index builder
   * has not yet been run.
   *
   * @return the location of the index
   */
  T getIndexLocation();

  /**
   * Flags that genes should be included in the construction of this index.
   * <p/>
   * This is true by default.
   *
   * @param genes whether to include genes in this build
   */
  void setIncludeGenes(boolean genes);

  boolean getIncludeGenes();

  /**
   * Flags that experiments should be included in the construction of this
   * index.
   * <p/>
   * This is true by default.
   *
   * @param experiments whether to include experiments in this build
   */
  void setIncludeExperiments(boolean experiments);

  boolean getIncludeExperiments();

  /**
   * Flags that only experiments or genes pending indexing should be included.
   * The pending flag is set within the Atlas database as a means of tracking
   * all experiments that have been loaded but not yet indexed. Setting "pending
   * mode" to true indicates you only wish to index these experiments.  Note
   * that you would normally only ever use pending mode whne updating the
   * index.
   *
   * This is false by default.
   *
   * @param pendingMode include only those experiments or genes flagged in the
   *                    backing database as "pending".  True indicates only
   *                    pending experiments are used, false indicates all.
   */
  void setPendingMode(boolean pendingMode);

  /**
   * Indicates whether experiments are included in the construction of this
   * index.  The pending flag indicates whether you wish to include only
   * experiments marked as pending, or all of them.
   *
   * @return include only those experiments flagged in the backing database as
   *         "pending".  True indicates only pending experiments are used, false
   *         indicates all.
   */
  boolean getPendingMode();

  /**
   * Build the index.  This will build the index entirely from scratch.  Use
   * this if you wish to create or recreate the index with up-to-date
   * information from the backing database.  If you wish to update the index
   * with only pre-existing genes and experiments, use {@link #updateIndex()}
   *
   * @throws IndexBuilderException if there was an error whilst building the
   *                               index
   */
  void buildIndex() throws IndexBuilderException;

  /**
   * Incrementally builds the index, updating the existing items rather than
   * building from scratch.
   *
   * @throws IndexBuilderException if there was an error whilst updating the
   *                               index
   */
  void updateIndex() throws IndexBuilderException;
}

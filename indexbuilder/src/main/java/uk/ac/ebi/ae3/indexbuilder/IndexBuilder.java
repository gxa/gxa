package uk.ac.ebi.ae3.indexbuilder;

import uk.ac.ebi.ae3.indexbuilder.listener.IndexBuilderListener;

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
 * <p/>
 * If you are using an IndexBuilder in a standalone application (not a web
 * application) and you do not want to reuse IndexBuilder for multiple index
 * building calls, you should make sure you register a listener that performs a
 * {@link #shutdown()} upon completion.  This will allow any resources being
 * used by the IndexBuilder implementation to be reclaimed.  Otherwise, an
 * IndexBuilder instance may run indefinitely.
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
   * <p/>
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
   * Initialise this IndexBuilder and any resources required by it.
   *
   * @throws IndexBuilderException if initialisation of this index builder
   *                               failed for any reason
   */
  void startup() throws IndexBuilderException;

  /**
   * Shutdown this IndexBuilder, and release any resources used by it
   *
   * @throws IndexBuilderException if shutdown of this index builder failed for
   *                               any reason
   */
  void shutdown() throws IndexBuilderException;

  /**
   * Build the index.  This will build the index entirely from scratch.  Use
   * this if you wish to create or recreate the index with up-to-date
   * information from the backing database.  If you wish to update the index
   * with only pre-existing genes and experiments, use {@link #updateIndex()}.
   * <p/>
   * Note that this method is not guaranteed to be synchronous, it only
   * guarantees that the index has started building.  Implementations are free
   * to define their own multithreaded strategies for index construction.  If
   * you wish to be notified on completion, you should register a listener to
   * get callback events when the build completes by using {@link
   * #buildIndex(uk.ac.ebi.ae3.indexbuilder.listener.IndexBuilderListener)}. You
   * can also use a listener to get at any errors that may have occurred during
   * index building.
   * <p/>
   * Calling this method is equivalent to calling <code>buildIndex(null)</code>.
   */
  void buildIndex();

  /**
   * Build the index and register a listener that provides a callback on
   * completion of the build task.  This will build the index entirely from
   * scratch.  Use this if you wish to create or recreate the index with
   * up-to-date information from the backing database.  If you wish to update
   * the index with only pre-existing genes and experiments, use {@link
   * #updateIndex()}.
   * <p/>
   * Note that this method is not guaranteed to be synchronous, it only
   * guarantees that the index has started building.  Implementations are free
   * to define their own multithreaded strategies for index construction.
   *
   * @param listener a listener that can be used to supply callbacks when
   *                 building of the index completes, or when any errors occur.
   */
  void buildIndex(IndexBuilderListener listener);

  /**
   * Incrementally builds the index, updating the existing items rather than
   * building from scratch.
   * <p/>
   * Note that this method is not guaranteed to be synchronous, it only
   * guarantees that the index has started updating.  Implementations are free
   * to define their own multithreaded strategies for index construction. If you
   * wish to be notified on completion, you should register a listener to get
   * callback events when the update completes by using {@link
   * #updateIndex(uk.ac.ebi.ae3.indexbuilder.listener.IndexBuilderListener)}.
   * You can also use a listener to get at any errors that may have occurred
   * during index building.
   * <p/>
   * Calling this method is equivalent to calling <code>updateIndex(null)</code>.
   */
  void updateIndex();

  /**
   * Incrementally builds the index, updating the existing items rather than
   * building from scratch.
   * <p/>
   * Note that this method is not guaranteed to be synchronous, it only
   * guarantees that the index has started updating.  Implementations are free
   * to define their own multithreaded strategies for index construction.
   *
   * @param listener a listener that can be used to supply callbacks when
   *                 updating of the index completes, or when any errors occur.
   */
  void updateIndex(IndexBuilderListener listener);
}

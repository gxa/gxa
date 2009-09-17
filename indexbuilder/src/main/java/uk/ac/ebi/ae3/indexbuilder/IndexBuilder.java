package uk.ac.ebi.ae3.indexbuilder;

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
   * Flags that genes should be included in the construction of this index
   *
   * @param genes whether to include genes in this build
   */
  void setIncludeGenes(boolean genes);

  boolean getIncludeGenes();

  /**
   * Flags that experiments should be included in the construction of this
   * index.
   * <p/>
   * Equivalent to <code>includeExperiments(experiments, false)</code, and this
   * is also the default.
   *
   * @param experiments whether to include experiments in this build
   */
  void setIncludeExperiments(boolean experiments);

  boolean getncludeExperiments();

  /**
   * Flags that experiments should be included in the construction of this
   * index.  The pending flag indicates whether you wish to include only
   * experiments marked as pending, or all of them.
   * <p/>
   * <code>includeExperiments(experiments, false)</code> is equivalent to
   * <code>includeExperiments(experiments)</code> and is also the default
   * option.
   *
   * @param pendingMode include only those experiments flagged in the backing
   *                    database as "pending".  True indicates only pending
   *                    experiments are used, false indicates all.
   */
  void setPendingMode(boolean pendingMode);

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

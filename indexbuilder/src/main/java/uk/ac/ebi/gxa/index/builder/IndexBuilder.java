package uk.ac.ebi.gxa.index.builder;

import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener;

import java.util.List;

/**
 * Interface for building a Gene Expression Atlas index.  Implementations should provide a way of setting the index
 * location, which may be of a generic type to allow the index to be backed by a database, file system, or some other
 * storage medium. IndexBuilder implementations should implement {@link #buildIndex()} which contains the logic to
 * construct the index.
 * <p/>
 * By default, all genes and experiments are included, and all experiments (both pending and non-pending) are included.
 * <p/>
 * If you are using an IndexBuilder in a standalone application (not a web application) and you do not want to reuse
 * IndexBuilder for multiple index building calls, you should make sure you register a listener that performs a {@link
 * #shutdown()} upon completion.  This will allow any resources being used by the IndexBuilder implementation to be
 * reclaimed.  Otherwise, an IndexBuilder instance may run indefinitely.
 *
 * @author Tony Burdett
 * @date 20-Aug-2009
 */
public interface IndexBuilder {
    /**
     * Set collection of index names to build
     * @param includeIndexes collection of names
     */
    void setIncludeIndexes(List<String> includeIndexes);

    /**
     * Get collection of index names to build
     *
     * @return set of names
     */
    List<String> getIncludeIndexes();

    /**
     * Initialise this IndexBuilder and any resources required by it.
     *
     * @throws IndexBuilderException if initialisation of this index builder failed for any reason
     */
    void startup() throws IndexBuilderException;

    /**
     * Shutdown this IndexBuilder, and release any resources used by it
     *
     * @throws IndexBuilderException if shutdown of this index builder failed for any reason
     */
    void shutdown() throws IndexBuilderException;

    /**
     * Build the index.  This will build the index entirely from scratch.  Use this if you wish to create or recreate
     * the index with up-to-date information from the backing database.
     * <p/>
     * Note that this method is not guaranteed to be synchronous, it only guarantees that the index has started
     * building. Implementations are free to define their own multithreaded strategies for index construction.  If you
     * wish to be notified on completion, you should register a listener to get callback events when the build completes
     * by using {@link #buildIndex(uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener)}. You can also use a
     * listener to get at any errors that may have occurred during index building.
     * <p/>
     * Calling this method is equivalent to calling <code>buildIndex(null)</code>.
     */
    void buildIndex();

    /**
     * Build the index and register a listener that provides a callback on completion of the build task.  This will
     * build the index entirely from scratch.  Use this if you wish to create or recreate the index with up-to-date
     * information from the backing database.
     * <p/>
     * Note that this method is not guaranteed to be synchronous, it only guarantees that the index has started
     * building. Implementations are free to define their own multithreaded strategies for index construction.
     * <p/>
     * The listener supplied will provide callbacks whenever the indexbuilder has interesting events to report.
     *
     * @param listener a listener that can be used to supply callbacks when building of the index completes, or when any
     *                 errors occur.
     */
    void buildIndex(IndexBuilderListener listener);

    /**
     * Incrementally builds the index, updating the existing index with new items rather than building from scratch.
     * <p/>
     * Note that this method only guarantees that the index has started updating. Implementations are free to define
     * their own multithreaded strategies for index construction, and should not block waiting for completion.  If you
     * wish to be notified on completion, you should register a listener to get callback events when the update
     * completes by using {@link #updateIndex(uk.ac.ebi.gxa.index.builder.listener.IndexBuilderListener)}. You can also
     * use a listener to get at any errors that may have occurred during index building.
     * <p/>
     * Calling this method is equivalent to calling <code>updateIndex(null)</code>.
     */
    void updateIndex();

    /**
     * Incrementally builds the index, updating the existing index with new items rather than building from scratch.
     * <p/>
     * Note that this method only guarantees that the index has started updating. Implementations are free to define
     * their own multithreaded strategies for index construction, and should not block waiting for completion.
     * <p/>
     * The listener supplied will provide callbacks whenever the indexbuilder has interesting events to report.
     *
     * @param listener a listener that can be used to supply callbacks when updating of the index completes, or when any
     *                 errors occur.
     */
    void updateIndex(IndexBuilderListener listener);

    /**
     * Register index update handler. Those handlers will be called when index build starts and finishes.
     * @param handler handler to register
     */
    void registerIndexBuildEventHandler(IndexBuilderEventHandler handler);

    /**
     * Unregister index update handler. Handlers will not receive notifications anymore.
     * @param handler handler to unregister
     */
    void unregisterIndexBuildEventHandler(IndexBuilderEventHandler handler);
}

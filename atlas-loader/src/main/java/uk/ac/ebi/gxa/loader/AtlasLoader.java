package uk.ac.ebi.gxa.loader;

import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;

/**
 * Interface for loading experiments and array designs into the Atlas.  Loaders require access to an {@link
 * uk.ac.ebi.microarray.atlas.dao.AtlasDAO} in order to read and write to the database.  They can also be configured
 * with a repository storing experiments to automate the loading process.  Implementations would then be free to
 * periodically poll this repository, looking for experiments that were not present int he database, and load them
 * automatically.
 * <p/>
 * This interface iss generically typed by two parameters, R and L.  R represents the type of resource the experiment
 * repository is: this will normally be a directory, URL, or possibly a datasource.  L represents the type of resources
 * this loader will load.  Again, this would normally be a File or a URL, but may be a string that is resolved in some
 * standard way against the experiment repository.  For example, an implementation may load String "accession numbers"
 * and extract the path to the file that represents that accession, given a standard way to resolve the supplied
 * accession number to the relevant file.
 *
 * @author Tony Burdett
 * @date 27-Nov-2009
 */
public interface AtlasLoader<R, L> {
    /**
     * Set the {@link uk.ac.ebi.microarray.atlas.dao.AtlasDAO} that will be used to read and write to the database
     * during data loading.
     *
     * @param atlasDAO the DAO that is used to obtain access to the database
     */
    void setAtlasDAO(AtlasDAO atlasDAO);

    /**
     * Set the {@link uk.ac.ebi.microarray.atlas.dao.AtlasDAO} that will be used to read and write to the database
     * during data loading.
     *
     * @return the DAO that is used to obtain access to the database
     */
    AtlasDAO getAtlasDAO();

    /**
     * Set the location for the experiment repository, that new experiments can be loaded from.  Note that
     * implementations do not necessarily have to do anything with this repository - some implementations may choose to
     * force load operations to specify the full path to the resource being loaded, rather than resolving an accession
     * number against the repository or automatically polling for changes.
     *
     * @param repositoryLocation the location of the experiment repository
     */
    void setRepositoryLocation(R repositoryLocation);

    /**
     * Get the location of the experiment repository.  This may be used, in some implementations, to automate loading or
     * in some cases to resolve accession numbers against.
     *
     * @return the location of the experiment repository
     */
    R getRepositoryLocation();

    /**
     * Sets the percentage of design elements that are allowed to be "missing" in the database before this load fails.
     * The percentage value - e.g. 0.25 - should be supplied here. Missing design elements occur when the data in the
     * database excludes certain design elements that may be referenced in the data file supplied.  This can happen for
     * valid reasons: for example, control spots on an array are often not recorded in the database.  This value sets
     * the percentage of design elements that are referenced in the data file but NOT the database.  If this percentage
     * is exceeded in any particular load, it will fail.
     * <p/>
     * AtlasLoaderService implementations should define sensible defaults for this cutoff - if it is not set here, the
     * default for the service implementation will be used.
     *
     * @param missingDesignElementsCutoff the percentage of design elements that are allowed to be absent in the
     *                                    database before a load fails.
     */
    void setMissingDesignElementsCutoff(double missingDesignElementsCutoff);

    /**
     * Gets the percentage of design elements that are allowed to be "missing" in the database before this load fails.
     * Missing design elements occur when the data in the database excludes certain design elements that may be
     * referenced in the data file supplied.  This can happen for valid reasons: for example, control spots on an array
     * are often not recorded in the database.  This value sets the percentage of design elements that are referenced in
     * the data file but NOT the database.  If this percentage is exceeded in any particular load, it will fail.
     *
     * @return the percentage of design elements that are allowed to be absent in the database before a load fails.
     */
    double getMissingDesignElementsCutoff();

    /**
     * Initializes this loader and any resources it requires.
     *
     * @throws AtlasLoaderException if startup fails for any reason
     */
    void startup() throws AtlasLoaderException;

    /**
     * Terminates this loader, and releases any resources it uses.
     *
     * @throws AtlasLoaderException if shutdown of this AtlasLoader failed for any reasone
     */
    void shutdown() throws AtlasLoaderException;

    /**
     * Perform a load operation on a reference to a particular experiment resource
     * <p/>
     * Note that this method is not guaranteed to be synchronous, it only guarantees that the load operation has
     * started.  Implementations are free to define their own multithreaded strategies for loading. If you wish to be
     * notified on completion, you should register a listener to get callback events when the build completes by using
     * {@link #loadExperiment(Object, uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener)}. You can also use a listener
     * to get at any errors that may have occurred during loading.
     * <p/>
     *
     * @param experimentResource the reference to the experiment you wish to load
     */
    void loadExperiment(L experimentResource);

    /**
     * Perform a load operation on a reference to a particular experiment resource
     * <p/>
     *
     * @param experimentResource the reference to the experiment you wish to load
     * @param listener           a listener that can be used to supply callbacks when loading of this experiment
     *                           completes, or when any errors occur.
     */
    void loadExperiment(L experimentResource, AtlasLoaderListener listener);

    /**
     * Perform a load operation on a reference to a particular array design resource
     * <p/>
     * Note that this method is not guaranteed to be synchronous, it only guarantees that the load operation has
     * started.  Implementations are free to define their own multithreaded strategies for loading. If you wish to be
     * notified on completion, you should register a listener to get callback events when the build completes by using
     * {@link #loadExperiment(Object, uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener)}. You can also use a listener
     * to get at any errors that may have occurred during loading.
     * <p/>
     *
     * @param arrayDesignResource the reference to the array design you wish to load
     */
    void loadArrayDesign(L arrayDesignResource);

    /**
     * Perform a load operation on a reference to a particular array design resource
     * <p/>
     *
     * @param arrayDesignResource the reference to the experiment you wish to load
     * @param listener            a listener that can be used to supply callbacks when loading of this experiment
     *                            completes, or when any errors occur.
     */
    void loadArrayDesign(L arrayDesignResource, AtlasLoaderListener listener);
}

package uk.ac.ebi.gxa.analytics.generator;

import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;

/**
 * Interface for modifying NetCDFs to include statistical analytics data required by the Atlas interface.
 * Implementations should provide a way of setting the NetCDF repository location, which may be of a generic type to
 * allow the NetCDFs to be built into a repository backed by a database, file system, or some other datasource.
 * Implementing classes should provide the method {@link #generateAnalytics()}, containing the logic to modify NetCDF
 * with analytics data
 *
 * @author Tony Burdett
 * @date 17-Sep-2009
 */
public interface AnalyticsGenerator<T> {
    /**
     * Set the {@link uk.ac.ebi.microarray.atlas.dao.AtlasDAO} that will be used to obtain data to generate NetCDFs.
     *
     * @param atlasDAO the DAO that is used to obtain data to generate NetCDFs from
     */
    void setAtlasDAO(AtlasDAO atlasDAO);

    /**
     * Get the {@link uk.ac.ebi.microarray.atlas.dao.AtlasDAO} that will be used to obtain data to generate NetCDFs.
     *
     * @return the Atlas 2 compliant datasource to generate NetCDFs from
     */
    AtlasDAO getAtlasDAO();

    /**
     * Set the location for the repository.  If there is already a pre-existing repository at this location,
     * implementations should update it.  If there is no repository pre-existing, it should be created.
     *
     * @param repositoryLocation the location of the repository
     */
    void setRepositoryLocation(T repositoryLocation);

    /**
     * Get the location of the repository.  This may not exist, if the NetCDF generator has not yet been run.
     *
     * @return the location of the repository
     */
    T getRepositoryLocation();

    /**
     * Set the {@link uk.ac.ebi.gxa.R.AtlasRFactory} that will be used to obtain {@link org.kchine.r.server.RServices}
     * for this generator.
     *
     * @param atlasRFactory the AtlasRFactory to acquire {@link org.kchine.r.server.RServices} from
     */
    void setAtlasRFactory(AtlasRFactory atlasRFactory);

    /**
     * Set the {@link uk.ac.ebi.gxa.R.AtlasRFactory} that will be used to obtain {@link org.kchine.r.server.RServices}
     * for this generator.
     *
     * @return the AtlasRFactory to acquire {@link org.kchine.r.server.RServices} from
     */
    AtlasRFactory getAtlasRFactory();

    /**
     * Initialise this IndexBuilder and any resources required by it.
     *
     * @throws AnalyticsGeneratorException if initialisation of this index builder failed for any reason
     */
    void startup() throws AnalyticsGeneratorException;

    /**
     * Shutdown this IndexBuilder, and release any resources used by it
     *
     * @throws AnalyticsGeneratorException if shutdown of this index builder failed for any reason
     */
    void shutdown() throws AnalyticsGeneratorException;


    /**
     * Run the NetCDFGenerator as a standalone, complete mechanism to generate all possible NetCDFs.  If you wish to
     * only regenerate a small slice of data, use {@link #generateAnalyticsForExperiment(String)}.
     * <p/>
     * Note that this method is not guaranteed to be synchronous, it only guarantees that the generation of NetCDFs has
     * started.  Implementations are free to define their own multithreaded strategies for NetCDF generation. If you
     * wish to be notified on completion, you should register a listener to get callback events when the build completes
     * by using {@link #generateAnalytics(uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener)}. You
     * can also use a listener to get at any errors that may have occurred during NetCDF generation.
     * <p/>
     * Calling this method is equivalent to calling <code>generateNetCDFs(null)</code>.
     */
    void generateAnalytics();

    /**
     * Run the NetCDFGenerator as a standalone, complete mechanism to generate all possible NetCDFs.  If you wish to
     * only regenerate a small slice of data, use {@link #generateAnalyticsForExperiment(String)}
     *
     * @param listener a listener that can be used to supply callbacks when generation of the NetCDF repository
     *                 completes, or when any errors occur.
     */
    void generateAnalytics(AnalyticsGeneratorListener listener);

    /**
     * Run the NetCDFGenerator to regenerate NetCDFs for only one particular accession.
     * <p/>
     * Note that this method is not guaranteed to be synchronous, it only guarantees that the generation of NetCDFs has
     * started.  Implementations are free to define their own multithreaded strategies for NetCDF generation. If you
     * wish to be notified on completion, you should register a listener to get callback events when the build completes
     * by using {@link #generateAnalyticsForExperiment(String, uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener)}.
     * You can also use a listener to get at any errors that may have occurred during NetCDF generation.
     * <p/>
     * Calling this method is equivalent to calling <code>generateNetCDFsForExperiment(experimentAccession,
     * null)</code>.
     *
     * @param experimentAccession the accession of the experiment to generate
     */
    void generateAnalyticsForExperiment(String experimentAccession);

    /**
     * Run the NetCDFGenerator to regenerate NetCDFs for only one particular accession.
     *
     * @param experimentAccession the accession of the experiment to generate
     * @param listener            a listener that can be used to supply callbacks when generation of the NetCDF for this
     *                            experiment completes, or when any errors occur.
     */
    void generateAnalyticsForExperiment(String experimentAccession,
                                        AnalyticsGeneratorListener listener);
}

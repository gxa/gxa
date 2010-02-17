package uk.ac.ebi.gxa.netcdf.generator;

import uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGeneratorListener;

/**
 * Interface for building a NetCDFs, as required by the Atlas expression analytics.  Implementations should provide a
 * way of setting the NetCDF repository location, which may be of a generic type to allow the NetCDFs to be built into a
 * repository backed by a database, file system, or some other datasource.  Any implementation should implement {@link
 * #generateNetCDFs()} which contains the logic to construct the NetCDF repository.
 *
 * @author Tony Burdett
 * @date 17-Sep-2009
 */
public interface NetCDFGenerator {
    /**
     * Initialise this NetCDFGenerator and any resources required by it.
     *
     * @throws NetCDFGeneratorException if initialisation of this index builder failed for any reason
     */
    void startup() throws NetCDFGeneratorException;

    /**
     * Shutdown this IndexBuilder, and release any resources used by it
     *
     * @throws NetCDFGeneratorException if shutdown of this index builder failed for any reason
     */
    void shutdown() throws NetCDFGeneratorException;


    /**
     * Run the NetCDFGenerator as a standalone, complete mechanism to generate all possible NetCDFs.  If you wish to
     * only regenerate a small slice of data, use {@link #generateNetCDFsForExperiment(String)}.
     * <p/>
     * Note that this method is not guaranteed to be synchronous, it only guarantees that the generation of NetCDFs has
     * started.  Implementations are free to define their own multithreaded strategies for NetCDF generation. If you
     * wish to be notified on completion, you should register a listener to get callback events when the build completes
     * by using {@link #generateNetCDFs(uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGeneratorListener)}. You can
     * also use a listener to get at any errors that may have occurred during NetCDF generation.
     * <p/>
     * Calling this method is equivalent to calling <code>generateNetCDFs(null)</code>.
     */
    void generateNetCDFs();

    /**
     * Run the NetCDFGenerator as a standalone, complete mechanism to generate all possible NetCDFs.  If you wish to
     * only regenerate a small slice of data, use {@link #generateNetCDFsForExperiment(String)}
     *
     * @param listener a listener that can be used to supply callbacks when generation of the NetCDF repository
     *                 completes, or when any errors occur.
     */
    void generateNetCDFs(NetCDFGeneratorListener listener);

    /**
     * Run the NetCDFGenerator to regenerate NetCDFs for only one particular accession.
     * <p/>
     * Note that this method is not guaranteed to be synchronous, it only guarantees that the generation of NetCDFs has
     * started.  Implementations are free to define their own multithreaded strategies for NetCDF generation. If you
     * wish to be notified on completion, you should register a listener to get callback events when the build completes
     * by using {@link #generateNetCDFsForExperiment(String, uk.ac.ebi.gxa.netcdf.generator.listener.NetCDFGeneratorListener)}.
     * You can also use a listener to get at any errors that may have occurred during NetCDF generation.
     * <p/>
     * Calling this method is equivalent to calling <code>generateNetCDFsForExperiment(experimentAccession,
     * null)</code>.
     *
     * @param experimentAccession the accession of the experiment to generate
     */
    void generateNetCDFsForExperiment(String experimentAccession);

    /**
     * Run the NetCDFGenerator to regenerate NetCDFs for only one particular accession.
     *
     * @param experimentAccession the accession of the experiment to generate
     * @param listener            a listener that can be used to supply callbacks when generation of the NetCDF for this
     *                            experiment completes, or when any errors occur.
     */
    void generateNetCDFsForExperiment(String experimentAccession,
                                      NetCDFGeneratorListener listener);
}

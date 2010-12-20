/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.analytics.generator;

import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;

/**
 * Interface for modifying NetCDFs to include statistical analytics data required by the Atlas interface.
 * Implementations should provide a way of setting the NetCDF repository location, which may be of a generic type to
 * allow the NetCDFs to be built into a repository backed by a database, file system, or some other datasource.
 * Implementing classes should provide the method {@link #generateAnalytics()}, containing the logic to modify NetCDF
 * with analytics data
 *
 * @author Tony Burdett
 */
public interface AnalyticsGenerator {

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

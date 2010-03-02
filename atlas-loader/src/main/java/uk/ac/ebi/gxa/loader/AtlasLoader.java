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

package uk.ac.ebi.gxa.loader;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;

import java.util.List;

/**
 * Interface for loading experiments and array designs into the Atlas.  Loaders require access to an {@link
 * uk.ac.ebi.gxa.dao.AtlasDAO} in order to read and write to the database.  They can also be configured with a
 * repository storing experiments to automate the loading process.  Implementations would then be free to periodically
 * poll this repository, looking for experiments that were not present int he database, and load them automatically.
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
public interface AtlasLoader<L> {
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
     * Sets whether reloads are permissible by this loader.  If true, experiments that are already present in the
     * backing datasource will be reloaded unchecked (with an optional warning log statement).  If false, attempting to
     * reload an existing experiment will cause an exception to be thrown.
     *
     * @param allowReloading true if reloads are permissible, false otherwise
     */
    void setAllowReloading(boolean allowReloading);

    /**
     * Gets whether reloads are permissible by this loader.  If true, experiments that are already present in the
     * backing datasource will be reloaded unchecked (with an optional warning log statement).  If false, attempting to
     * reload an existing experiment will cause an exception to be thrown.
     *
     * @return whether or not reloads are allowed - true indicates they are
     */
    boolean getAllowReloading();

    /**
     * Sets a list of type names that can be used to identify genes within the Atlas, in priority order.  Examples of
     * values that might be entered here are "ensembl", "entrez" etc.  These strings repres
     *
     * @return
     */
    List<String> getGeneIdentifierPriority();

    void setGeneIdentifierPriority(List<String> geneIdentifierPriority);

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

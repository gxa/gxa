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

import uk.ac.ebi.gxa.loader.listener.AtlasLoaderListener;

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
 */
public interface AtlasLoader {
    /**
     * Sets whether reloads are permissible by this loader.  If true, experiments that are already present in the
     * backing datasource will be reloaded unchecked (with an optional warning log statement).  If false, attempting to
     * reload an existing experiment will cause an exception to be thrown.
     *
     * @param allowReloading true if reloads are permissible, false otherwise
     */
//    void setAllowReloading(boolean allowReloading);

    /**
     * Terminates this loader, and releases any resources it uses.
     *
     * @throws AtlasLoaderException if shutdown of this AtlasLoader failed for any reasone
     */
    void shutdown() throws AtlasLoaderException;

    /**
     * Perform a load operation according to provided command and listener asynchronously
     * <p/>
     *
     * @param command  command to process
     * @param listener           a listener that can be used to supply callbacks when loading of this experiment
     *                           completes, or when any errors occur.
     */
    void doCommand(AtlasLoaderCommand command, AtlasLoaderListener listener);
}

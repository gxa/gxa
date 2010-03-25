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

package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.AtlasLoader;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;

import java.io.File;

/**
 * An abstract Atlas loader service, containing basic setup that is required across all loader implementations.  This
 * leaves implementing classes free to describe only the logic required to perform loads.
 *
 * @author Tony Burdett
 * @date 27-Nov-2009
 */
public abstract class AtlasLoaderService<T> {
    private DefaultAtlasLoader atlasLoader;
    private boolean allowReloading = false;

    // logging
    private Logger log = LoggerFactory.getLogger(this.getClass());

    protected AtlasLoaderService(DefaultAtlasLoader atlasLoader) {
        this.atlasLoader = atlasLoader;
    }

    protected Logger getLog() {
        return log;
    }

    protected AtlasDAO getAtlasDAO() {
        return atlasLoader.getAtlasDAO();
    }

    protected File getAtlasNetCDFRepo() {
        return atlasLoader.getAtlasNetCDFRepo();
    }

    protected DefaultAtlasLoader getAtlasLoader() {
        return atlasLoader;
    }

    protected boolean allowReloading() {
        return allowReloading;
    }

    /**
     * Sets whether or not reloads should be suppressed by this load service.  If this is set to true, attempting to
     * reload an existing experiment will cause an exception.  If false, reloads will procede like any other load
     * (although a warning should be issued to the log stream by implementations of this class).
     *
     * @param allowReloading whether or not to automatically allow reloads
     */
    public void setAllowReloading(boolean allowReloading) {
        this.allowReloading = allowReloading;
    }

    /**
     * Perform a load on the given loader resource.  Normally, experiment and array design loaders will be separate
     * implementations of this class so there is not a requirement to separate out the load methods.
     *
     * @param loaderResource the resource to load
     * @param listener listener
     * @throws AtlasLoaderServiceException if failed
     */
    public abstract void load(T loaderResource, AtlasLoaderServiceListener listener) throws AtlasLoaderServiceException;
}

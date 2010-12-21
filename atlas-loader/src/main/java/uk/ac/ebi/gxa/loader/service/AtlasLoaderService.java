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
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;

import java.io.File;

/**
 * An abstract Atlas loader service, containing basic setup that is required across all loader service implementations.  This
 * leaves implementing classes free to describe only the logic required to perform loads.
 *
 * @author Tony Burdett
 */
public abstract class AtlasLoaderService {
    final private DefaultAtlasLoader atlasLoader;

    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    protected AtlasLoaderService(DefaultAtlasLoader atlasLoader) {
        this.atlasLoader = atlasLoader;
    }

    final protected Logger getLog() {
        return log;
    }

    final protected AtlasDAO getAtlasDAO() {
        return atlasLoader.getAtlasDAO();
    }

    final public AtlasComputeService getComputeService() {
        return atlasLoader.getComputeService();
    }

    final protected File getAtlasNetCDFDirectory(String experimentAccession) {
        return getAtlasNetcdfDAO().getDataDirectory(experimentAccession);
    }

    protected AtlasNetCDFDAO getAtlasNetcdfDAO() {
        return atlasLoader.getAtlasNetCDFDAO();
    }

    final protected DefaultAtlasLoader getAtlasLoader() {
        return atlasLoader;
    }

    final protected boolean allowReloading() {
        return atlasLoader.getAllowReloading();
    }
}

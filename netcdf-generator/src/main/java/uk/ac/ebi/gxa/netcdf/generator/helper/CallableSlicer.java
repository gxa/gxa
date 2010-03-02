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

package uk.ac.ebi.gxa.netcdf.generator.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.Gene;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * An implementation of Callable that performs data slciing tasks common in the
 * constructiuon of NetCDF files.  THis abstract class contains methods to set
 * the required resources (namely, an {@link ExecutorService} and and {@link
 * uk.ac.ebi.gxa.dao.AtlasDAO} as well as the prefetched data,
 * genes and analytics.  As implementations may wish to update the collections
 * of genes and expression analytics that are not mapped to any design elements,
 * these fields are available
 *
 * @author Tony Burdett
 * @date 28-Oct-2009
 */
public abstract class CallableSlicer<T> implements Callable<T> {
    // service for running task
    private final ExecutorService service;

    // required DAO
    private AtlasDAO atlasDAO;

    // logger
    private Logger log = LoggerFactory.getLogger(getClass());

    public CallableSlicer(ExecutorService service) {
        this.service = service;
    }

    public ExecutorService getService() {
        return service;
    }

    public Logger getLog() {
        return log;
    }

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }
}

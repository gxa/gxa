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

package uk.ac.ebi.gxa.analytics.generator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.generator.AnalyticsGeneratorException;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;

/**
 * An abstract AnalyticsGeneratorService, that provides convenience methods for getting and setting parameters required
 * across all AnalyticsGenerator implementations.  This class is typed by the type of the repository backing this
 * AnalyticsGeneratorService - this may be a file, a datasource, an FTP directory, or something else. Implementing
 * classes have access to this repository and an {@link uk.ac.ebi.gxa.dao.AtlasDAO} that provides
 * interaction with the Atlas database (following an Atlas 2 schema).
 * <p/>
 * All implementing classes should provide the method {@link #createAnalytics()} which contains the logic for
 * constructing the relevant parts of the index for each implementation.  Clients should call {@link
 * #generateAnalytics()} to trigger Analytics construction.  At the moment, this method simply delegates to the abstract
 * form, but extra initialisation may go in this method.
 *
 * @author Tony Burdett
 */
public abstract class AnalyticsGeneratorService {
    private final AtlasDAO atlasDAO;
    private final AtlasNetCDFDAO atlasNetCDFDAO;
    private final AtlasComputeService atlasComputeService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public AnalyticsGeneratorService(AtlasDAO atlasDAO, AtlasNetCDFDAO atlasNetCDFDAO, AtlasComputeService atlasComputeService) {
        this.atlasDAO = atlasDAO;
        this.atlasNetCDFDAO = atlasNetCDFDAO;
        this.atlasComputeService = atlasComputeService;
    }

    protected Logger getLog() {
        return log;
    }

    protected AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    protected AtlasNetCDFDAO getAtlasNetCDFDAO() {
        return atlasNetCDFDAO;
    }

    protected AtlasComputeService getAtlasComputeService() {
        return atlasComputeService;
    }

    public void generateAnalytics() throws AnalyticsGeneratorException {
        createAnalytics();
    }

    public void generateAnalyticsForExperiment(String experimentAccession, AnalyticsGeneratorListener listener)
            throws AnalyticsGeneratorException {
        createAnalyticsForExperiment(experimentAccession, listener);
    }

    protected abstract void createAnalytics() throws AnalyticsGeneratorException;

    protected abstract void createAnalyticsForExperiment(String experimentAccession, AnalyticsGeneratorListener listener) throws AnalyticsGeneratorException;
}

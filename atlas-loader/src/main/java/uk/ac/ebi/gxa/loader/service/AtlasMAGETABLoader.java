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
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ScanNode;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.data.NetCDFDataCreator;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.LoadExperimentCommand;
import uk.ac.ebi.gxa.loader.UnloadExperimentCommand;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.loader.steps.*;
import uk.ac.ebi.gxa.utils.ZipUtil;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.utils.FileUtil.*;

/**
 * A Loader application that will insert data from MAGE-TAB format files into the Atlas backend database.
 * <p/>
 * This class should be configured with a {@link javax.sql.DataSource} in order to load data.  This datasource should be
 * an oracle database conforming to the Atlas DB schema, and connection pooling should be externally managed.
 * <p/>
 * This loader can be used either as a standalone application or as part of the atlas-web infrastructure.  IN the first
 * case, using spring to configure the datasource and connection pooling is probably the easiest way to ensure good
 * performance.
 *
 * @author Tony Burdett
 */
public class AtlasMAGETABLoader {
    private static final Logger log = LoggerFactory.getLogger(AtlasMAGETABLoader.class);

    private AtlasComputeService atlasComputeService;
    private AtlasDataDAO atlasDataDAO;
    private LoaderDAO dao;

    private AtlasExperimentUnloaderService unloaderService;

    /**
     * Load a MAGE-TAB format document at the given URL into the Atlas DB.
     *
     * @param cmd      command
     * @param listener a listener that can report on load completion or error events
     * @throws uk.ac.ebi.gxa.loader.AtlasLoaderException
     *          in case of any problem during loading
     */
    public void process(LoadExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        if (listener != null)
            listener.setProgress("Running AtlasMAGETABLoader");

        URL idfFileLocation = cmd.getUrl();

        // create a cache for our objects
        AtlasLoadCache cache = new AtlasLoadCache();

        cache.setAvailQTypes(cmd.getPossibleQTypes());

        File tempDirectory = null;
        try {
            if (idfFileLocation.getFile().endsWith(".zip")) {
                try {
                    tempDirectory = createTempDirectory("magetab-loader");
                    ZipUtil.decompress(idfFileLocation, tempDirectory);

                    File[] idfs = tempDirectory.listFiles(extension("idf", true));
                    if (idfs == null) {
                        throw new AtlasLoaderException("The directory has suddenly disappeared or is not readable");
                    }
                    if (idfs.length == 0) {
                        throw new AtlasLoaderException("No IDFs to import!");
                    }
                    idfFileLocation = new URL("file:" + idfs[0]);
                } catch (IOException ex) {
                    throw new AtlasLoaderException(ex);
                }
            }

            try {
                // Parsing itself
                logProgress(listener, 1, ParsingStep.displayName());
                final MAGETABInvestigation investigation = new ParsingStep().parse(idfFileLocation);

                // Getting an experiment
                logProgress(listener, 2, CreateExperimentStep.displayName());
                cache.setExperiment(new CreateExperimentStep().readExperiment(investigation, cmd.getUserData()));

                // Samples
                logProgress(listener, 3, SourceStep.displayName());
                new SourceStep().readSamples(investigation, cache, dao);

                // Assays
                logProgress(listener, 4, AssayAndHybridizationStep.displayName());
                new AssayAndHybridizationStep().readAssays(investigation, cache, dao);

                //use raw data
                Collection<String> useRawData = cmd.getUserData().get("useRawData");
                if (useRawData != null && useRawData.size() == 1 && "true".equals(useRawData.iterator().next())) {
                    logProgress(listener, 5, ArrayDataStep.displayName());
                    new ArrayDataStep().readArrayData(atlasComputeService, investigation, listener, cache);
                    log.info("Raw data are used; processed data will not be processed");
                } else {
                    logProgress(listener, 6, DerivedArrayDataMatrixStep.displayName());
                    new DerivedArrayDataMatrixStep().readProcessedData(investigation, cache);
                }

                //load RNA-seq experiment
                //ToDo: add condition based on "getUserData"
                if (isHTS(investigation)) {
                    logProgress(listener, 7, HTSArrayDataStep.displayName());
                    new HTSArrayDataStep().readHTSData(investigation, atlasComputeService, cache, dao);
                }
            } catch (AtlasLoaderException e) {
                // something went wrong - no objects have been created though
                log.error("There was a problem whilst trying to build atlas model from " + idfFileLocation, e);
                throw e;
            }

            if (listener != null) {
                listener.setProgress("Storing experiment to DB");
            }
            write(listener, cache);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            // TODO: 4alf: proper handling!!!
            throw new AtlasLoaderException(e);
        } finally {
            if (tempDirectory != null)
                deleteDirectory(tempDirectory);
        }
    }

    private void logProgress(AtlasLoaderServiceListener listener, int index, String displayName) {
        final String progress = "Step " + ++index + " of 8: " + displayName;
        listener.setProgress(progress);
        log.info(progress);
    }

    private void write(AtlasLoaderServiceListener listener, AtlasLoadCache cache) throws AtlasLoaderException {
        // parsing completed, so now write the objects in the cache
        try {
            writeObjects(cache, listener);

            if (listener != null) {
                listener.setProgress("Done");
                if (cache.fetchExperiment() != null) {
                    listener.setAccession(cache.fetchExperiment().getAccession());
                }
            }
        } catch (AtlasLoaderException e) {
            throw e;
        } catch (Exception e) {
            throw new AtlasLoaderException(e);
        }
    }

    void writeObjects(AtlasLoadCache cache, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        int numOfObjects = (cache.fetchExperiment() == null ? 0 : 1)
                + cache.fetchAllSamples().size() + cache.fetchAllAssays().size();

        // validate the load(s)
        validateLoad(cache);


        // check experiment exists in database, and not just in the loadmonitor
        String experimentAccession = cache.fetchExperiment().getAccession();

        try {
            dao.getExperiment(experimentAccession);
            // experiment genuinely was already in the DB, so remove old experiment
            log.info("Deleting existing version of experiment " + experimentAccession);
            try {
                if (listener != null)
                    listener.setProgress("Unloading existing version of experiment " + experimentAccession);
                unloaderService.process(new UnloadExperimentCommand(experimentAccession), listener);
            } catch (AtlasLoaderException e) {
                throw new AtlasLoaderException(e);
            }
        } catch (RecordNotFoundException e) {
            // do nothing - experiment matching experimentAccession not found is a valid situation here
        }

        // start the load(s)
        try {
            // now write the cleaned up data
            log.info("Writing experiment " + experimentAccession);

            dao.save(cache.fetchExperiment());
            writeExperimentNetCDF(cache, listener);

            // and return true - everything loaded ok
            log.info("Writing " + numOfObjects + " objects completed successfully");
        } catch (Throwable t) {
            log.error("Error!", t);
            throw new AtlasLoaderException(t);
        }
    }

    private void writeExperimentNetCDF(AtlasLoadCache cache, AtlasLoaderServiceListener listener) throws AtlasDataException {
        final Experiment experiment = cache.fetchExperiment();
        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(experiment);

        try {
            for (final ArrayDesign shallowArrayDesign : experiment.getArrayDesigns()) {
                Collection<Assay> adAssays = experiment.getAssaysForDesign(shallowArrayDesign);
                log.info("Starting NetCDF for {} and {} ({} assays)",
                        new Object[]{experiment.getAccession(), shallowArrayDesign.getAccession(), adAssays.size()});

                if (listener != null)
                    listener.setProgress("Writing NetCDF for " + experiment.getAccession() +
                            " and " + shallowArrayDesign);

                final NetCDFDataCreator dataCreator = ewd.getDataCreator(dao.getArrayDesign(shallowArrayDesign.getAccession()));
                dataCreator.setAssayDataMap(cache.getAssayDataMap());

                dataCreator.createNetCdf();

                if (dataCreator.hasWarning() && listener != null) {
                    for (String warning : dataCreator.getWarnings()) {
                        listener.setWarning(warning);
                    }
                }
                log.info("Finalising NetCDF changes for {} and {}", experiment.getAccession(), shallowArrayDesign.getAccession());
            }
        } finally {
            closeQuietly(ewd);
        }
    }

    private void validateLoad(AtlasLoadCache cache) throws AtlasLoaderException {
        try {
            if (cache.fetchExperiment() == null)
                throw new AtlasLoaderException("Cannot load without an experiment");

            if (cache.fetchAllAssays().isEmpty())
                throw new AtlasLoaderException("No assays found");

            for (Assay assay : cache.fetchAllAssays()) {
                if (assay.hasNoProperties())
                    throw new AtlasLoaderException("Assay " + assay.getAccession() + " has no properties! All assays need at least one.");

                if (!cache.getAssayDataMap().containsKey(assay.getAccession()))
                    throw new AtlasLoaderException("Assay " + assay.getAccession() + " contains no data! All assays need some.");

                if (assay.getSamples().isEmpty())
                    throw new AtlasLoaderException("No sample for assay " + assay.getAccession() + " found");
            }

            if (cache.fetchAllSamples().isEmpty())
                throw new AtlasLoaderException("No samples found");

            for (Sample sample : cache.fetchAllSamples())
                if (sample.getAssayAccessions().isEmpty())
                    throw new AtlasLoaderException("No assay for sample " + sample.getAccession() + " found");
        } catch (AtlasLoaderException e) {
            log.warn("Problem during loading: " + e.getMessage());
            throw e;
        }
    }

    public void setLoaderDAO(LoaderDAO dao) {
        this.dao = dao;
    }

    public void setAtlasComputeService(AtlasComputeService atlasComputeService) {
        this.atlasComputeService = atlasComputeService;
    }

    public void setAtlasDataDAO(AtlasDataDAO atlasDataDAO) {
        this.atlasDataDAO = atlasDataDAO;
    }

    public void setUnloaderService(AtlasExperimentUnloaderService unloaderService) {
        this.unloaderService = unloaderService;
    }

    public static boolean isHTS(MAGETABInvestigation investigation) {
        // check that data is from RNASeq (comments: "Comment [ENA_RUN]"    "Comment [FASTQ_URI]" must be present)
        Collection<ScanNode> scanNodes = investigation.SDRF.getNodes(ScanNode.class);
        if (scanNodes.size() == 0) {
            log.info("No comment scan nodes found - investigation {} is not HTS", investigation.getAccession());
            return false;
        }
        for (ScanNode scanNode : scanNodes) {
            if (!(scanNode.comments.keySet().contains("ENA_RUN") && scanNode.comments.containsKey("FASTQ_URI"))) {
                log.info("No comment[ENA_RUN] found - investigation {} is not HTS", investigation.getAccession());
                return false;
            }
        }
        return true;
    }
}

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

import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.gxa.dao.LoadStage;
import uk.ac.ebi.gxa.dao.LoadStatus;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreator;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFCreatorException;
import uk.ac.ebi.gxa.utils.ValueListHashMap;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.LoadExperimentCommand;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.UnloadExperimentCommand;
import uk.ac.ebi.gxa.loader.steps.*;
import uk.ac.ebi.microarray.atlas.model.*;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;

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
 * @date 26-Aug-2009
 */
public class AtlasMAGETABLoader extends AtlasLoaderService<LoadExperimentCommand> {
    public AtlasMAGETABLoader(DefaultAtlasLoader loader) {
        super(loader);
    }

    /**
     * Load a MAGE-TAB format document at the given URL into the Atlas DB.
     *
     * @param cmd command
     * @param listener a listener that can report on load completion or error events
     */
    public void process(LoadExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        final URL idfFileLocation = cmd.getUrl();

        // create a cache for our objects
        AtlasLoadCache cache = new AtlasLoadCache();

        cache.setAvailQTypes(cmd.getPossibleQTypes());

        // create an investigation ready to parse to
        MAGETABInvestigationExt investigation = new MAGETABInvestigationExt();

        // pair this cache and this investigation in the registry
        AtlasLoadCacheRegistry.getRegistry().registerExperiment(investigation, cache);

        try {
            AtlasLoaderUtils.WatcherThread watcher = AtlasLoaderUtils.createProgressWatcher(investigation, listener);

            final ArrayList<Step> steps = new ArrayList<Step>();
            steps.add(new ParsingStep(idfFileLocation, investigation));
            steps.add(new CreateExperimentStep(investigation));
            steps.add(new SourceStep(investigation));
            steps.add(new AssayAndHybridizationStep(investigation));

            //use raw data
            String[] useRawData = cmd.getUserData().get("useRawData");
            if (useRawData != null && useRawData.length == 1 && "true".equals(useRawData[0])) {
                steps.add(new ArrayDataStep(this, investigation));
            }
            steps.add(new DerivedArrayDataMatrixStep(investigation));

            //load RNA-seq experiment
            //ToDo: add condition based on "getUserData"
            steps.add(new HTSArrayDataStep(investigation, this.getComputeService()));

            try {
                int index = 0;
                for (Step s : steps) {
                    if (listener != null) {
                        listener.setProgress("Step " + ++index + " of " + steps.size() + ": " + s.displayName());
                        getLog().info("Step " + index + " of " + steps.size() + ": " + s.displayName());
                    }
                    s.run();
                }
            } catch (AtlasLoaderException e) {
                // something went wrong - no objects have been created though
                getLog().error("There was a problem whilst trying to build atlas model from " + idfFileLocation, e);
                throw e;
            } finally {
                if (watcher != null) {
                    watcher.stopWatching();
                }
            }

            if (listener != null) {
                listener.setProgress("Storing experiment to DB");
            }

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
        finally {
            try {
                AtlasLoadCacheRegistry.getRegistry().deregisterExperiment(investigation);
            } catch(Exception e) {
                // skip
            }
            try {
                cache.clear();
            } catch(Exception e) {
                // skip 
            }
        }
    }

    protected void writeObjects(AtlasLoadCache cache, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        int numOfObjects = (cache.fetchExperiment() == null ? 0 : 1)
                + cache.fetchAllSamples().size() + cache.fetchAllAssays().size();

        // validate the load(s)
        validateLoad(cache);


        // check experiment exists in database, and not just in the loadmonitor
        String experimentAccession = cache.fetchExperiment().getAccession();
        if (getAtlasDAO().getExperimentByAccession(experimentAccession) != null) {
            // experiment genuinely was already in the DB, so remove old experiment
            getLog().info("Deleting existing version of experiment " + experimentAccession);
            try {
                if(listener != null)
                    listener.setProgress("Unloading existing version of experiment " + experimentAccession);
                new AtlasExperimentUnloaderService(getAtlasLoader()).process(
                        new UnloadExperimentCommand(experimentAccession), listener
                );
            } catch (AtlasLoaderException e) {
                throw new AtlasLoaderException(e);
            }
        }

        // start the load(s)
        boolean success = false;
        startLoad(experimentAccession);

        try {
            // now write the cleaned up data
            getLog().info("Writing " + numOfObjects + " objects to Atlas 2 datasource...");
            // first, load experiment
            long start = System.currentTimeMillis();
            getLog().info("Writing experiment " + experimentAccession);
            if(listener != null)
                listener.setProgress("Writing experiment " + experimentAccession);

            getAtlasDAO().writeExperiment(cache.fetchExperiment());
            long end = System.currentTimeMillis();
            String total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().info("Wrote experiment {} in {}s.", experimentAccession, total);

            // next, write assays
            start = System.currentTimeMillis();
            getLog().info("Writing " + cache.fetchAllAssays().size() + " assays");
            if(listener != null)
                listener.setProgress("Writing " + cache.fetchAllAssays().size() + " assays");

            for (Assay assay : cache.fetchAllAssays()) {
                getAtlasDAO().writeAssay(assay);
            }
            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().info("Wrote {} assays in {}s.", cache.fetchAllAssays().size(), total);

            // finally, load samples
            start = System.currentTimeMillis();
            getLog().info("Writing " + cache.fetchAllSamples().size() + " samples");
            if(listener != null)
                listener.setProgress("Writing " + cache.fetchAllSamples().size() + " samples");
            for (Sample sample : cache.fetchAllSamples()) {
                if (sample.getAssayAccessions() != null && sample.getAssayAccessions().size() > 0) {
                    getAtlasDAO().writeSample(sample, experimentAccession);
                }
            }
            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().info("Wrote {} samples in {}s.", cache.fetchAllAssays().size(), total);

            // write data to netcdf
            start = System.currentTimeMillis();
            getLog().info("Writing NetCDF...");
            writeExperimentNetCDF(cache, listener);
            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().info("Wrote NetCDF in {}s.", total);

            // and return true - everything loaded ok
            getLog().info("Writing " + numOfObjects + " objects completed successfully");
            success = true;
        } catch (Throwable t) {
            getLog().error("Error!", t);
            throw new AtlasLoaderException(t);
        }
        finally {
            // end the load(s)
            endLoad(experimentAccession, success);
        }
    }

    private void writeExperimentNetCDF(AtlasLoadCache cache, AtlasLoaderServiceListener listener) throws NetCDFCreatorException {
        List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(cache.fetchExperiment().getAccession());

        ValueListHashMap<String, Assay> assaysByArrayDesign = new ValueListHashMap<String, Assay>();
        for(Assay assay : assays) {
            String adAcc = assay.getArrayDesignAccession();
            if(null != adAcc) {
                assaysByArrayDesign.put(adAcc, assay);
            } else {
                throw new NetCDFCreatorException("ArrayDesign accession missing");
            }
        }

        Experiment experiment = getAtlasDAO().getExperimentByAccession(cache.fetchExperiment().getAccession());
        String version = getAtlasLoader().getVersionFromMavenProperties();

        for(String adAcc : assaysByArrayDesign.keySet()) {
            List<Assay> adAssays = assaysByArrayDesign.get(adAcc);
            getLog().info("Starting NetCDF for " + cache.fetchExperiment().getAccession() +
                    " and " + adAcc + " (" + adAssays.size() + " assays)");

            if(listener != null)
                listener.setProgress("Writing NetCDF for " +  cache.fetchExperiment().getAccession() +
                    " and " + adAcc);

            NetCDFCreator netCdfCreator = new NetCDFCreator();

            netCdfCreator.setAssays(adAssays);
            for (Assay assay : adAssays)
                for (Sample sample : getAtlasDAO().getSamplesByAssayAccession(assay.getAccession()))
                    netCdfCreator.setSample(assay, sample);

            netCdfCreator.setArrayDesign(getAtlasDAO().getArrayDesignByAccession(adAcc));
            netCdfCreator.setExperiment(experiment);
            netCdfCreator.setAssayDataMap(cache.getAssayDataMap());
            netCdfCreator.setVersion(version);

            netCdfCreator.createNetCdf(getAtlasNetCDFDirectory(experiment.getAccession()));
            if(netCdfCreator.hasWarning() && listener != null) {
                for(String warning : netCdfCreator.getWarnings())
                    listener.setWarning(warning);
            }
            
            getLog().info("Finalising NetCDF changes for " + cache.fetchExperiment().getAccession() +
                    " and " + adAcc);
        }
    }

    private void validateLoad(AtlasLoadCache cache)
            throws AtlasLoaderException {
        if (cache.fetchExperiment() == null) {
            String msg = "Cannot load without an experiment";
            getLog().error(msg);
            throw new AtlasLoaderException(msg);
        }

        checkExperiment(cache.fetchExperiment().getAccession());

        if(cache.fetchAllAssays().isEmpty())
            throw new AtlasLoaderException("No assays found");

        Set<String> referencedArrayDesigns = new HashSet<String>();
        for (Assay assay : cache.fetchAllAssays()) {
            if (!referencedArrayDesigns.contains(assay.getArrayDesignAccession())) {
                if (!checkArray(assay.getArrayDesignAccession())) {
                    String msg = "The array design " + assay.getArrayDesignAccession() + " was not found in the " +
                            "database: it is prerequisite that referenced arrays are present prior to " +
                            "loading experiments";
                    getLog().error(msg);
                    throw new AtlasLoaderException(msg);
                }

                referencedArrayDesigns.add(assay.getArrayDesignAccession());
            }

            if (assay.getProperties() == null || assay.getProperties().size() == 0) {
                throw new AtlasLoaderException("Assay " + assay.getAccession() + " has no properties! All assays need at least one.");
            }

            if(!cache.getAssayDataMap().containsKey(assay.getAccession()))
                throw new AtlasLoaderException("Assay " + assay.getAccession() + " contains no data! All assays need some.");
        }

        if(cache.fetchAllSamples().isEmpty())
            throw new AtlasLoaderException("No samples found");

        Set<String> sampleReferencedAssays = new HashSet<String>();
        for(Sample sample : cache.fetchAllSamples()) {
            if (sample.getAssayAccessions() == null || sample.getAssayAccessions().isEmpty())
                throw new AtlasLoaderException("No assays for sample " + sample.getAccession() + " found");
            else
                sampleReferencedAssays.addAll(sample.getAssayAccessions());
        }

        for(Assay assay : cache.fetchAllAssays())
            if(!sampleReferencedAssays.contains(assay.getAccession()))
                throw new AtlasLoaderException("No sample for assay " + assay.getAccession() + " found");

        // all checks passed if we got here
    }

    private void checkExperiment(String accession) throws AtlasLoaderException {
        // check load_monitor for this accession
        getLog().debug("Fetching load details for " + accession);
        LoadDetails loadDetails = getAtlasDAO().getLoadDetailsForExperimentsByAccession(accession);
        if (loadDetails != null) {
            getLog().info("Found load details for " + accession);
            // if we are suppressing reloads, check the details further
            if (!allowReloading()) {
                getLog().info("Load details present, reloads not allowed...");
                // there are details: load is valid only if the load status is "pending" or "failed"
                boolean pending = loadDetails.getStatus().equalsIgnoreCase(LoadStatus.PENDING.toString());
                if(pending)
                    throw new AtlasLoaderException("Experiment is in PENDING state");

                boolean priorFailure = loadDetails.getStatus().equalsIgnoreCase(LoadStatus.FAILED.toString());
                if (priorFailure) {
                    String msg = "Experiment " + accession + " was previously loaded, but failed.  " +
                            "Any bad data will be overwritten";
                    getLog().warn(msg);
                    throw new AtlasLoaderException(msg);
                }
            }
            else {
                // not suppressing reloads, so continue
                getLog().warn("Experiment " + accession + " was previously loaded, but reloads are not " +
                        "automatically suppressed");
            }
        }
        else {
            // no experiment present in load_monitor table
            getLog().debug("No load details obtained");
        }
    }

    private boolean checkArray(String accession) {
        // check load_monitor for this accession
        getLog().debug("Fetching array design for " + accession);
        ArrayDesign arrayDesign = getAtlasDAO().getArrayDesignByAccession(accession);
        if (arrayDesign == null) {
            // this array design is absent
            getLog().debug("DAO lookup returned null for " + accession);
            return false;
        }
        else {
            getLog().debug("DAO lookup found array design " + accession);
            return true;
        }
    }

    private void startLoad(String accession) {
        getLog().info("Updating load_monitor: starting load for " + accession);
        getAtlasDAO().writeLoadDetails(accession, LoadStage.LOAD, LoadStatus.WORKING);
    }

    private void endLoad(String accession, boolean success) {
        getLog().info("Updating load_monitor: ending load for " + accession);
        getAtlasDAO().writeLoadDetails(accession, LoadStage.LOAD, (success ? LoadStatus.DONE : LoadStatus.FAILED));
    }
}

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

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import ucar.nc2.NetcdfFileWriteable;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.AccessionHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.InvestigationTitleHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonAffiliationHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonLastNameHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.*;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.LoadStage;
import uk.ac.ebi.gxa.dao.LoadStatus;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.handler.idf.AtlasLoadingAccessionHandler;
import uk.ac.ebi.gxa.loader.handler.idf.AtlasLoadingInvestigationTitleHandler;
import uk.ac.ebi.gxa.loader.handler.idf.AtlasLoadingPersonAffiliationHandler;
import uk.ac.ebi.gxa.loader.handler.idf.AtlasLoadingPersonLastNameHandler;
import uk.ac.ebi.gxa.loader.handler.sdrf.*;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.netcdf.generator.NetCDFGeneratorException;
import uk.ac.ebi.gxa.netcdf.generator.helper.DataSlice;
import uk.ac.ebi.gxa.netcdf.generator.helper.NetCDFFormatter;
import uk.ac.ebi.gxa.netcdf.generator.helper.NetCDFWriter;
import uk.ac.ebi.microarray.atlas.model.*;

import java.io.File;
import java.io.IOException;
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
public class AtlasMAGETABLoader extends AtlasLoaderService<URL> {
    private double missingDesignElementsCutoff = 1.0;
    private File atlasNetCDFRepo;

    public AtlasMAGETABLoader(AtlasDAO atlasDAO, File atlasNetCDFRepo) {
        super(atlasDAO);

        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }

    protected double getMissingDesignElementsCutoff() {
        return missingDesignElementsCutoff;
    }

    /**
     * Sets the percentage of design elements that are allowed to be "missing" in the database before this load fails.
     * This is set at 1.0 (i.e. 100%) by default, so no job will ever fail.  You should normally override this, as high
     * percentages of missing design elements usually indicates an error, either in the datafile or else during array
     * design loading.
     *
     * @param missingDesignElementsCutoff the percentage of design elements that are allowed to be absent in the
     *                                    database before a load fails.
     */
    public void setMissingDesignElementsCutoff(double missingDesignElementsCutoff) {
        this.missingDesignElementsCutoff = missingDesignElementsCutoff;
    }

    /**
     * Load a MAGE-TAB format document at the given URL into the Atlas DB.
     *
     * @param idfFileLocation the location of the idf part of the MAGETAB document you want to load.
     * @param listener
     * @return true if loading suceeded, false if loading failed
     */
    public void load(URL idfFileLocation, AtlasLoaderServiceListener listener) throws AtlasLoaderServiceException {
        // create a cache for our objects
        AtlasLoadCache cache = new AtlasLoadCache();

        // create an investigation ready to parse to
        MAGETABInvestigation investigation = new MAGETABInvestigation();

        // pair this cache and this investigation in the registry
        AtlasLoadCacheRegistry.getRegistry().registerExperiment(investigation, cache);

        try {
            // configure the handlers so we write out the right bits
            configureHandlers();

            // now, perform the parse - with registered handlers, our cache will be populated
            MAGETABParser parser = new MAGETABParser();
            parser.setParsingMode(ParserMode.READ_AND_WRITE);

            // register an error item listener
            parser.addErrorItemListener(new ErrorItemListener() {
                public void errorOccurred(ErrorItem item) {
                    // lookup message
                    String message = "";
                    for (ErrorCode ec : ErrorCode.values()) {
                        if (item.getErrorCode() == ec.getIntegerValue()) {
                            message = ec.getErrorMessage();
                            break;
                        }
                    }
                    if (message.equals("")) {
                        if (item.getComment().equals("")) {
                            message = "Unknown error";
                        }
                        else {
                            message = item.getComment();
                        }
                    }
                    String comment = item.getComment();

                    // log the error
                    // todo: this should go to a different log stream, part of loader report -
                    // probably should dynamically creating an appender that writes to the magetab directory
                    getLog().error(
                            "Parser reported:\n\t" +
                                    item.getErrorCode() + ": " + message + " (" + comment + ")\n\t\t- " +
                                    "occurred in parsing " + item.getParsedFile() + " " +
                                    "[line " + item.getLine() + ", column " + item.getCol() + "].", item);
                }
            });

            AtlasLoaderUtils.WatcherThread watcher = AtlasLoaderUtils.createProgressWatcher(investigation, listener);
            try {
                parser.parse(idfFileLocation, investigation);
                getLog().info("Parsing finished");
            }
            catch (ParseException e) {
                // something went wrong - no objects have been created though
                getLog().error("There was a problem whilst trying to parse " + idfFileLocation, e);
                throw new AtlasLoaderServiceException("Parse error: " + e.getErrorItem().toString(), e);
            } finally {
                if(watcher != null)
                    watcher.stopWatching();
            }

            if (listener != null) {
                listener.setProgress("Storing experiment to DB");
            }

            // parsing completed, so now write the objects in the cache
            try {
                if(4 == 5)
                writeObjects(cache);

                if (listener != null) {
                    listener.setProgress("Done");
                    if (cache.fetchExperiment() != null) {
                        listener.setAccession(cache.fetchExperiment().getAccession());
                    }
                }
            } catch (AtlasLoaderServiceException e) {
                throw e;
            } catch (Exception e) {
                throw new AtlasLoaderServiceException(e);
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

    protected void configureHandlers() {
        HandlerPool pool = HandlerPool.getInstance();

        // calibrate the parser with the relevent handlers that can load atlas data
        pool.replaceHandlerClass(AccessionHandler.class,
                                 AtlasLoadingAccessionHandler.class);
        pool.replaceHandlerClass(InvestigationTitleHandler.class,
                                 AtlasLoadingInvestigationTitleHandler.class);
        pool.replaceHandlerClass(PersonAffiliationHandler.class,
                                 AtlasLoadingPersonAffiliationHandler.class);
        pool.replaceHandlerClass(PersonLastNameHandler.class,
                                 AtlasLoadingPersonLastNameHandler.class);
        pool.replaceHandlerClass(SourceHandler.class,
                                 AtlasLoadingSourceHandler.class);
        pool.replaceHandlerClass(AssayHandler.class,
                                 AtlasLoadingAssayHandler.class);
        pool.replaceHandlerClass(HybridizationHandler.class,
                                 AtlasLoadingHybridizationHandler.class);
        pool.replaceHandlerClass(FactorValueNodeHandler.class,
                                 AtlasLoadUpdatingFactorValueNodeHandler.class);
        pool.replaceHandlerClass(DerivedArrayDataMatrixHandler.class,
                                 AtlasLoadingDerivedArrayDataMatrixHandler.class);
    }

    protected void writeObjects(AtlasLoadCache cache) throws AtlasLoaderServiceException {
        int numOfObjects = (cache.fetchExperiment() == null ? 0 : 1)
                + cache.fetchAllSamples().size() + cache.fetchAllAssays().size();

        // validate the load(s)
        validateLoad(cache.fetchExperiment(), cache.fetchAllAssays());

        // start the load(s)
        boolean success = false;
        startLoad(cache.fetchExperiment().getAccession());

        try {
            // prior to writing, do some data cleanup to handle missing design elements.
            // this is workaround for legacy data, can be removed when loader is improved
            getLog().info("Cleaning up data - removing any expression values linked " +
                    "to design elements missing from the database");
            long start = System.currentTimeMillis();

            Map<String,Map<Integer, String>> deAccsByAD = new HashMap<String,Map<Integer,String>>();
            Map<String,Map<Integer, String>> deNamesByAD = new HashMap<String,Map<Integer,String>>();

            for(Assay assay : cache.fetchAllAssays()) {
                // get the array design for this assay
                String arrayDesignAccession = assay.getArrayDesignAccession();

                if(!deAccsByAD.containsKey(arrayDesignAccession)) {
                    deAccsByAD.put(arrayDesignAccession,
                        getAtlasDAO().getDesignElementsByArrayAccession(arrayDesignAccession));

                    deNamesByAD.put(arrayDesignAccession,
                        getAtlasDAO().getDesignElementNamesByArrayAccession(arrayDesignAccession));
                }

                // TODO: pretty dirty here, assumes all assays have the same DE content
                Map<String, Float> evsByDEref = assay.getExpressionValuesByDesignElementReference();
                Map<Integer,Float> evsByDEID = new HashMap<Integer,Float>();

                deAccsByAD.get(arrayDesignAccession).values().retainAll(evsByDEref.keySet());
                for (Map.Entry<Integer, String> deAcc : deAccsByAD.get(arrayDesignAccession).entrySet()) {
                    evsByDEID.put(deAcc.getKey(), evsByDEref.get(deAcc.getValue()));
                }

                deNamesByAD.get(arrayDesignAccession).values().retainAll(evsByDEref.keySet());
                for (Map.Entry<Integer, String> deName : deNamesByAD.get(arrayDesignAccession).entrySet()) {
                    evsByDEID.put(deName.getKey(), evsByDEref.get(deName.getValue()));
                }

                if(evsByDEID.size() == 0) {
                    getLog().info("No design elements found in DB for array design " + arrayDesignAccession);
                }
                assay.setExpressionValues(evsByDEID);
            }
            long end = System.currentTimeMillis();

            String total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().info("Data cleanup took " + total + "s.");

            // now write the cleaned up data
            getLog().info("Writing " + numOfObjects + " objects to Atlas 2 datasource...");

            // first, load experiment
            start = System.currentTimeMillis();
            getLog().info("Writing experiment " + cache.fetchExperiment().getAccession());
            getAtlasDAO().writeExperiment(cache.fetchExperiment());
            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().info("Wrote experiment {} in {}s.", cache.fetchExperiment().getAccession(), total);

            // next, write assays
            start = System.currentTimeMillis();
            getLog().info("Writing " + cache.fetchAllAssays().size() + " assays");
            for (Assay assay : cache.fetchAllAssays()) {
                getAtlasDAO().writeAssay(assay);
            }
            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().info("Wrote {} assays in {}s.", cache.fetchAllAssays().size(), total);

            // finally, load samples
            start = System.currentTimeMillis();
            getLog().info("Writing " + cache.fetchAllSamples().size() + " samples");
            for (Sample sample : cache.fetchAllSamples()) {
                if (sample.getAssayAccessions().size() > 0) {
                    String experimentAccession = cache.fetchExperiment().getAccession();
                    getAtlasDAO().writeSample(sample, experimentAccession);
                }
                else {
                    throw new AtlasLoaderServiceException("No assays for sample found");
                }
            }

            // write data to netcdf
            try {
                writeExperimentNetCDF(cache);
            } catch (NetCDFGeneratorException e) {
                getLog().error("Failed to generate netcdf", e);
            } catch (IOException e) {
                getLog().error("Failed to generate netcdf", e);
            }

            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().info("Wrote {} samples in {}s.", cache.fetchAllAssays().size(), total);

            // and return true - everything loaded ok
            getLog().info("Writing " + numOfObjects + " objec" +
                    "ts completed successfully");
            success = true;
        }
        finally {
            // end the load(s)
            endLoad(cache.fetchExperiment().getAccession(), success);
        }
    }

    private void writeExperimentNetCDF(AtlasLoadCache cache) throws NetCDFGeneratorException, IOException {
        List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(cache.fetchExperiment().getAccession());

        for(Assay assay : assays) {
            Assay cacheAssay = cache.fetchAssay(assay.getAccession());
            assay.setExpressionValues(cacheAssay.getExpressionValues());
            assay.setExpressionValuesByDesignElementReference(cacheAssay.getExpressionValuesByDesignElementReference());
        }

        Map<String, List<Assay>> assaysByArrayDesign = new HashMap<String,List<Assay>>();

        for(Assay assay : assays) {
            String adAcc = assay.getArrayDesignAccession();
            if(null != adAcc) {
                if(!assaysByArrayDesign.containsKey(adAcc))
                    assaysByArrayDesign.put(adAcc, new ArrayList<Assay>());

                assaysByArrayDesign.get(adAcc).add(assay);
            } else {
                throw new NetCDFGeneratorException("ArrayDesign accession missing");
            }
        }

        for(String adAcc : assaysByArrayDesign.keySet()) {
            // create a data slicer to slice up this experiment
            DataSlice dataSlice = new DataSlice(cache.fetchExperiment(),
                                                getAtlasDAO().getArrayDesignByAccession(adAcc));

            dataSlice.storeAssays(assaysByArrayDesign.get(adAcc));

            for (Assay assay : assaysByArrayDesign.get(adAcc)) {
                // fetch samples for this assay
                List<Sample> samples = getAtlasDAO().getSamplesByAssayAccession(assay.getAccession());
                for (Sample sample : samples) {
                    // and store
                    dataSlice.storeSample(assay, sample);
                }
            }

            Map<Integer, Map<Integer,Float>> evs = new HashMap<Integer,Map<Integer,Float>>();

            for(Assay assay : assaysByArrayDesign.get(adAcc)) {
                evs.put(assay.getAssayID(),
                        assay.getExpressionValues());
            }

            dataSlice.storeExpressionValues(evs);

            NetcdfFileWriteable netCDF = createNetCDF(
                    dataSlice.getExperiment(),
                    dataSlice.getArrayDesign());

            // format it with paramaters suitable for our data
            NetCDFFormatter formatter = new NetCDFFormatter();
            formatter.formatNetCDF(netCDF, dataSlice);

            // actually create the netCDF
            netCDF.create();

            // write the data from our data slice to this netCDF
            try {
                NetCDFWriter writer = new NetCDFWriter();
                writer.writeNetCDF(netCDF, dataSlice);
            }
            finally {
                // save and close the netCDF
                netCDF.close();
            }
            getLog().info("Finalising NetCDF changes for " + dataSlice.getExperiment().getAccession() +
                    " and " + dataSlice.getArrayDesign().getAccession());
        }
    }

    private void validateLoad(Experiment experiment, Collection<Assay> assays) throws AtlasLoaderServiceException {
        if (experiment == null) {
            String msg = "Cannot load without an experiment";
            getLog().error(msg);
            throw new AtlasLoaderServiceException(msg);
        }

        checkExperiment(experiment.getAccession());

        Set<String> referencedArrayDesigns = new HashSet<String>();
        for (Assay assay : assays) {
            if (!referencedArrayDesigns.contains(assay.getArrayDesignAccession())) {
                if (!checkArray(assay.getArrayDesignAccession())) {
                    String msg = "The array design " + assay.getArrayDesignAccession() + " was not found in the " +
                            "database: it is prerequisite that referenced arrays are present prior to " +
                            "loading experiments";
                    getLog().error(msg);
                    throw new AtlasLoaderServiceException(msg);
                }

                referencedArrayDesigns.add(assay.getArrayDesignAccession());
            }
        }

        // all checks passed if we got here
    }

    private void checkExperiment(String accession) throws AtlasLoaderServiceException {
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
                    throw new AtlasLoaderServiceException("Experiment is in PENDING state");

                boolean priorFailure = loadDetails.getStatus().equalsIgnoreCase(LoadStatus.FAILED.toString());
                if (priorFailure) {
                    String msg = "Experiment " + accession + " was previously loaded, but failed.  " +
                            "Any bad data will be overwritten";
                    getLog().warn(msg);
                    throw new AtlasLoaderServiceException(msg);
                }
            }
            else {
                // not suppressing reloads, so continue
                getLog().warn("Experiment " + accession + " was previously loaded, but reloads are not " +
                        "automatically suppressed");

                // check experiment exists in database, and not just in the loadmonitor
                if (getAtlasDAO().getExperimentByAccession(accession) != null) {
                    // experiment genuinely was already in the DB, so remove old experiment
                    getLog().info("Deleting existing version of experiment " + accession);
                    getAtlasDAO().deleteExperiment(accession);
                }
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

    private NetcdfFileWriteable createNetCDF(Experiment experiment,
                                             ArrayDesign arrayDesign)
            throws IOException {

        // repository location exists?
        if (!getRepositoryLocation().exists()) {
            if (!getRepositoryLocation().mkdirs()) {
                throw new IOException("Could not read create directory at " +
                        getRepositoryLocation().getAbsolutePath());
            }
        }

        String netcdfName =
                experiment.getExperimentID() + "_" +
                        arrayDesign.getArrayDesignID() + ".nc";
        String netcdfPath =
                new File(getRepositoryLocation(), netcdfName).getAbsolutePath();
        NetcdfFileWriteable netcdfFile =
                NetcdfFileWriteable.createNew(netcdfPath, false);

        // add metadata global attributes
        netcdfFile.addGlobalAttribute(
                "CreateNetCDF_VERSION",
                "test");
        netcdfFile.addGlobalAttribute(
                "experiment_accession",
                experiment.getAccession());
//    netcdfFile.addGlobalAttribute(
//        "quantitationType",
//        qtType); // fixme: quantitation type lookup required
        netcdfFile.addGlobalAttribute(
                "ADaccession",
                arrayDesign.getAccession());
        netcdfFile.addGlobalAttribute(
                "ADid",
                arrayDesign.getArrayDesignID());
        netcdfFile.addGlobalAttribute(
                "ADname",
                arrayDesign.getName());

        return netcdfFile;
    }

    private File getRepositoryLocation() {
        return getAtlasNetCDFRepo();
    }

    public File getAtlasNetCDFRepo() {
        return atlasNetCDFRepo;
    }

    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }
}

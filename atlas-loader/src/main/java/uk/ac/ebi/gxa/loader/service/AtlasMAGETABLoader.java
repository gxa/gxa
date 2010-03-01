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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.loader.service;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
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
public class AtlasMAGETABLoader extends AtlasLoaderService<URL> {
    private double missingDesignElementsCutoff = 1.0;

    public AtlasMAGETABLoader(AtlasDAO atlasDAO) {
        super(atlasDAO);
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
     * @return true if loading suceeded, false if loading failed
     */
    public boolean load(URL idfFileLocation) {
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
                                    "[line " + item.getLine() + ", column " + item.getCol() + "].");
                }
            });

            try {
                parser.parse(idfFileLocation, investigation);
                getLog().debug("Parsing finished");
            }
            catch (ParseException e) {
                // something went wrong - no objects have been created though
                getLog().error("There was a problem whilst trying to parse " + idfFileLocation);
                e.printStackTrace();
                return false;
            }

            // parsing completed, so now write the objects in the cache
            return writeObjects(cache);
        }
        finally {
            AtlasLoadCacheRegistry.getRegistry().deregisterExperiment(investigation);
            cache.clear();
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

    protected boolean writeObjects(AtlasLoadCache cache) {
        int numOfObjects = cache.fetchAllExperiments().size() +
                cache.fetchAllSamples().size() +
                cache.fetchAllAssays().size();

        // validate the load(s)
        if (!validateLoad(cache.fetchAllExperiments(), cache.fetchAllAssays())) {
            return false;
        }

        // start the load(s)
        boolean success = false;
        for (Experiment exp : cache.fetchAllExperiments()) {
            startLoad(exp.getAccession());
        }

        try {
            // prior to writing, do some data cleanup to handle missing design elements.
            // this is workaround for legacy data, can be removed when loader is improved
            getLog().info("Cleaning up data - removing any expression values linked " +
                    "to design elements missing from the database");
            long start = System.currentTimeMillis();
            Map<String, Set<String>> designElementsByArray =
                    new HashMap<String, Set<String>>();
            int missingCount = 0;
            for (Assay assay : cache.fetchAllAssays()) {
                // get the array design for this assay
                String arrayDesignAcc = assay.getArrayDesignAccession();

                // check that this array design is loaded
                if (getAtlasDAO().getArrayDesignByAccession(arrayDesignAcc) == null) {
                    getLog().error(
                            "The array design " + arrayDesignAcc + " is not present in the database.  This array " +
                                    "MUST be loaded before experiments using this array can be loaded.");
                    return success = false;
                }

                // get the missing design elements - either DB lookup or fetch from map
                Set<String> missingDesignElements;
                try {
                    if (!designElementsByArray.containsKey(arrayDesignAcc)) {
                        if (assay.getExpressionValuesByDesignElementReference() == null) {
                            getLog().debug("Assay " + assay.getAssayID() + " contains no expression values");
                            missingDesignElements =
                                    lookupMissingDesignElements(
                                            new HashMap<String, Float>(),
                                            assay.getArrayDesignAccession());
                        }
                        else {
                            missingDesignElements =
                                    lookupMissingDesignElements(
                                            assay.getExpressionValuesByDesignElementReference(),
                                            assay.getArrayDesignAccession());

                            // add to our cache for known missing design elements
                            designElementsByArray.put(arrayDesignAcc, missingDesignElements);

                            missingCount += missingDesignElements.size();
                        }
                    }
                    else {
                        missingDesignElements = designElementsByArray.get(arrayDesignAcc);
                    }
                }
                catch (AtlasLoaderException e) {
                    // this occurs if we exceed the cutoff, so just return false
                    return success = false;
                }

                // finally, trim the missing design elements from this assay
                trimMissingDesignElements(assay, missingDesignElements);
            }
            getLog().info("Removed all expression values for " + missingCount +
                    " missing design elements from cache of assays to load");
            long end = System.currentTimeMillis();

            String total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().info("Data cleanup took " + total + "s.");

            // now write the cleaned up data
            getLog().info("Writing " + numOfObjects + " objects to Atlas 2 datasource...");

            // first, load experiments
            start = System.currentTimeMillis();
            getLog().debug("Writing " + cache.fetchAllExperiments().size() + " experiment(s)");
            System.out.print("Writing experiments...");
            for (Experiment experiment : cache.fetchAllExperiments()) {
                getAtlasDAO().writeExperiment(experiment);
                System.out.print(".");
            }
            System.out.println("done!");
            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().debug("Wrote {} experiments in {}s.", cache.fetchAllExperiments().size(), total);


            // next, write assays
            start = System.currentTimeMillis();
            getLog().debug("Writing " + cache.fetchAllAssays().size() + " assays");
            System.out.print("Writing assays...");
            for (Assay assay : cache.fetchAllAssays()) {
                getAtlasDAO().writeAssay(assay);
                System.out.print(".");
            }
            System.out.println("done!");
            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().debug("Wrote {} assays in {}s.", cache.fetchAllAssays().size(), total);

            // finally, load samples
            start = System.currentTimeMillis();
            getLog().debug("Writing " + cache.fetchAllSamples().size() + " samples");
            System.out.print("Writing samples...");
            for (Sample sample : cache.fetchAllSamples()) {
                getAtlasDAO().writeSample(sample);
                System.out.print(".");
            }
            System.out.println("done!");
            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().debug("Wrote {} samples in {}s.", cache.fetchAllAssays().size(), total);

            // and return true - everything loaded ok
            getLog().info("Writing " + numOfObjects + " objects completed successfully");
            return success = true;
        }
        catch (Exception e) {
            getLog().error("Writing " + numOfObjects + " objects failed: " + e.getMessage() +
                    "\nData may be left in an inconsistent state: rerun this load to overwrite.");
            e.printStackTrace();
            return success = false;
        }
        finally {
            // end the load(s)
            for (Experiment exp : cache.fetchAllExperiments()) {
                endLoad(exp.getAccession(), success);
            }
        }
    }

    private boolean validateLoad(Collection<Experiment> experiments, Collection<Assay> assays) {
        for (Experiment exp : experiments) {
            if (!checkExperiment(exp.getAccession())) {
                return false;
            }
        }
        Set<String> referencedArrayDesigns = new HashSet<String>();
        for (Assay assay : assays) {
            if (!referencedArrayDesigns.contains(assay.getArrayDesignAccession())) {
                if (!checkArray(assay.getArrayDesignAccession())) {
                    getLog().error("The array design " + assay.getArrayDesignAccession() + " was not found in the " +
                            "database: it is prerequisite that referenced arrays are present prior to " +
                            "loading experiments");
                    return false;
                }

                referencedArrayDesigns.add(assay.getArrayDesignAccession());
            }
        }

        // all checks passed if we got here
        return true;
    }

    private boolean checkExperiment(String accession) {
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
                boolean priorFailure = loadDetails.getStatus().equalsIgnoreCase(LoadStatus.FAILED.toString());
                if (priorFailure) {
                    getLog().warn("Experiment " + accession + " was previously loaded, but failed.  " +
                            "Any bad data will be overwritten");
                }
                return pending || priorFailure;
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

                return true;
            }
        }
        else {
            // no experiment present in load_monitor table
            getLog().debug("No load details obtained");
            return true;
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

    private Set<String> lookupMissingDesignElements(Map<String, Float> expressionValues, String arrayDesignAccession)
            throws AtlasLoaderException {
        // use our dao to lookup design elements, instead of the writer class
        Map<Integer, String> designElements =
                getAtlasDAO().getDesignElementsByArrayAccession(arrayDesignAccession);
        Map<Integer, String> designElementNames =
                getAtlasDAO().getDesignElementNamesByArrayAccession(arrayDesignAccession);

        // check off missing design elements against any present
        Set<String> missingDesignElements = new HashSet<String>();

        // for every expression value, check the design element ref is in database (first by accession, then name)
        Set<String> expressionValuesKeys = new HashSet<String>();
        expressionValuesKeys.addAll(expressionValues.keySet());
        for (String deRef : expressionValuesKeys) {
            if (!designElements.containsValue(deRef)) {
                // no design element with matching accession, so check name
                if (!designElementNames.containsValue(deRef)) {
                    // no design element with matching name either.  Definitely missing
                    // deAcc is missing - add to missing design elements and provide trace output
                    missingDesignElements.add(deRef);
                    getLog().trace("Design Element '" + deRef + "' is referenced in the data file, " +
                            "but is not present in the database.  This may be a control spot missing in legacy data");
                }
                else {
                    // the data we've obtained from the datafile reflects names, not accessions
                    // but this is ok, stored procedure still accepts them
                }
            }
        }

        // grab the number of design elements - total and missing
        int totalDEs = expressionValues.size();
        int missingDEs = missingDesignElements.size();

        // log the number of missing DEs for this array design
        double percentMissing = ((double) missingDEs / (double) totalDEs);
        String percentMissingStr = new DecimalFormat("#.#").format(percentMissing * 100);
        String percentCutoffStr = new DecimalFormat("#.#").format(getMissingDesignElementsCutoff() * 100);

        // if there are missing design elements, warn
        if (percentMissing > 0) {
            getLog().warn("Missing design elements for " + arrayDesignAccession + ": " +
                    missingDEs + "/" + totalDEs + " (" + percentMissingStr + " %)");
        }

        // check this percentage against the cut-off configured
        if (percentMissing > getMissingDesignElementsCutoff()) {
            String msg =
                    "The total number of missing design elements for exceeds allowed cutoff: " + percentMissingStr +
                            "% (max " + percentCutoffStr + "%)";
            getLog().error(msg);
            throw new AtlasLoaderException(msg);
        }

        return missingDesignElements;
    }

    private void trimMissingDesignElements(Assay assay, Set<String> missingDesignElements) {
        for (String deAcc : missingDesignElements) {
            if (assay.getExpressionValuesByDesignElementReference().containsKey(deAcc)) {
                getLog().trace("Missing design element " + deAcc + " will be " +
                        "removed from this assay - not in database.");
                assay.getExpressionValuesByDesignElementReference().remove(deAcc);
            }
        }
    }
}

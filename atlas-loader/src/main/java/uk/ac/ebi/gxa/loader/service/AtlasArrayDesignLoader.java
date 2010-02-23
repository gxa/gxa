package uk.ac.ebi.gxa.loader.service;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.AccessionHandler;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABArrayParser;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.LoadStage;
import uk.ac.ebi.gxa.dao.LoadStatus;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.handler.adf.AtlasLoadingAccessionHandler;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.LoadDetails;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 22-Feb-2010
 */
public class AtlasArrayDesignLoader extends AtlasLoaderService<URL> {
    private List<String> geneIdentifierPriority = new ArrayList<String>();

    protected AtlasArrayDesignLoader(AtlasDAO atlasDAO) {
        super(atlasDAO);
    }

    public List<String> getGeneIdentifierPriority() {
        return geneIdentifierPriority;
    }

    public void setGeneIdentifierPriority(List<String> geneIdentifierPriority) {
        this.geneIdentifierPriority = geneIdentifierPriority;
    }

    public boolean load(URL adfFileLocation) {
        // create a cache for our objects
        AtlasLoadCache cache = new AtlasLoadCache();

        // create an investigation ready to parse to
        MAGETABArrayDesign arrayDesign = new MAGETABArrayDesign();

        // pair this cache and this investigation in the registry
        AtlasLoadCacheRegistry.getRegistry().registerArrayDesign(arrayDesign, cache);

        try {
            // configure the handlers so we write out the right bits
            configureHandlers();

            // now, perform the parse - with registered handlers, our cache will be populated
            MAGETABArrayParser parser = new MAGETABArrayParser();
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
                parser.parse(adfFileLocation, arrayDesign);
                getLog().debug("Parsing finished");
            }
            catch (ParseException e) {
                // something went wrong - no objects have been created though
                getLog().error("There was a problem whilst trying to parse " + adfFileLocation);
                e.printStackTrace();
                return false;
            }

            // parsing completed, so now write the objects in the cache
            return writeObjects(cache);
        }
        finally {
            AtlasLoadCacheRegistry.getRegistry().deregisterArrayDesign(arrayDesign);
            cache.clear();
        }
    }

    protected void configureHandlers() {
        HandlerPool pool = HandlerPool.getInstance();

        // todo - calibrate the parser with the relevant handlers that can load atlas data
        pool.replaceHandlerClass(AccessionHandler.class,
                                 AtlasLoadingAccessionHandler.class);
    }

    protected boolean writeObjects(AtlasLoadCache cache) {
        int numOfObjects = cache.fetchAllExperiments().size() +
                cache.fetchAllSamples().size() +
                cache.fetchAllAssays().size();

        // validate the load(s)
        if (!validateLoad(cache.fetchAllArrayDesignBundles())) {
            return false;
        }

        // start the load(s)
        boolean success = false;
        for (ArrayDesignBundle bundle : cache.fetchAllArrayDesignBundles()) {
            startLoad(bundle.getAccession());
        }

        try {
            // write the data
            getLog().info("Writing " + numOfObjects + " objects to Atlas 2 datasource...");

            long start, end;
            String total;

            // load array design bundles
            start = System.currentTimeMillis();
            getLog().debug("Writing " + cache.fetchAllArrayDesignBundles().size() + " array design(s)");
            System.out.print("Writing array designs...");
            for (ArrayDesignBundle arrayBundle : cache.fetchAllArrayDesignBundles()) {
                // first, update the bundle with the identifier preferences
                arrayBundle.setGeneIdentifierNamesInPriorityOrder(getGeneIdentifierPriority());

                getAtlasDAO().writeArrayDesignBundle(arrayBundle);
                System.out.print(".");
            }
            System.out.println("done!");
            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().debug("Wrote {} experiments in {}s.", cache.fetchAllExperiments().size(), total);

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

    private boolean validateLoad(Collection<ArrayDesignBundle> arrayDesignBundles) {
        for (ArrayDesignBundle adb : arrayDesignBundles) {
            if (!checkArrayDesign(adb.getAccession())) {
                return false;
            }
        }

        // all checks passed if we got here
        return true;
    }

    private boolean checkArrayDesign(String accession) {
        // check load_monitor for this accession
        getLog().debug("Fetching load details for " + accession);
        LoadDetails loadDetails = getAtlasDAO().getLoadDetailsForArrayDesignsByAccession(accession);
        if (loadDetails != null) {
            getLog().info("Found load details for " + accession);
            // if we are suppressing reloads, check the details further
            if (!allowReloading()) {
                getLog().info("Load details present, reloads not allowed...");
                // there are details: load is valid only if the load status is "pending" or "failed"
                boolean pending = loadDetails.getStatus().equalsIgnoreCase(LoadStatus.PENDING.toString());
                boolean priorFailure = loadDetails.getStatus().equalsIgnoreCase(LoadStatus.FAILED.toString());
                if (priorFailure) {
                    getLog().warn("Array Design " + accession + " was previously loaded, but failed.  " +
                            "Any bad data will be overwritten");
                }
                return pending || priorFailure;
            }
            else {
                // not suppressing reloads, so continue
                getLog().warn("Array Design " + accession + " was previously loaded, but reloads are not " +
                        "automatically suppressed");

                // check experiment exists in database, and not just in the loadmonitor
                if (getAtlasDAO().getArrayDesignByAccession(accession) != null) {
                    // experiment genuinely was already in the DB, so remove old experiment
                    getLog().info("Deleting existing version of array design " + accession);
                    getAtlasDAO().deleteArrayDesign(accession);
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

    private void startLoad(String accession) {
        getLog().info("Updating load_monitor: starting load for " + accession);
        getAtlasDAO().writeLoadDetails(accession, LoadStage.LOAD, LoadStatus.WORKING);
    }

    private void endLoad(String accession, boolean success) {
        getLog().info("Updating load_monitor: ending load for " + accession);
        getAtlasDAO().writeLoadDetails(accession, LoadStage.LOAD, (success ? LoadStatus.DONE : LoadStatus.FAILED));
    }
}

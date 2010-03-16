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
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.AccessionHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.ArrayDesignNameHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.ProviderHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.TechnologyTypeHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.CompositeElementHandler;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABArrayParser;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.LoadStage;
import uk.ac.ebi.gxa.dao.LoadStatus;
import uk.ac.ebi.gxa.dao.LoadType;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.handler.adf.*;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;
import uk.ac.ebi.microarray.atlas.model.LoadDetails;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 22-Feb-2010
 */
public class AtlasArrayDesignLoader extends AtlasLoaderService<URL> {
    private List<String> geneIdentifierPriority = new ArrayList<String>();

    public AtlasArrayDesignLoader(AtlasDAO atlasDAO) {
        super(atlasDAO);
    }

    public List<String> getGeneIdentifierPriority() {
        return geneIdentifierPriority;
    }

    public void setGeneIdentifierPriority(List<String> geneIdentifierPriority) {
        this.geneIdentifierPriority = geneIdentifierPriority;
    }

    public boolean load(final URL adfFileLocation, final Listener listener) {
        // create a cache for our objects
        AtlasLoadCache cache = new AtlasLoadCache();

        // create an investigation ready to parse to
        final MAGETABArrayDesign arrayDesign = new MAGETABArrayDesign();

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

            AtlasLoaderUtils.WatcherThread watcher = AtlasLoaderUtils.createProgressWatcher(arrayDesign, listener);
            try {
                parser.parse(adfFileLocation, arrayDesign);
                getLog().info("Parsing finished");
            }
            catch (ParseException e) {
                // something went wrong - no objects have been created though
                getLog().error("There was a problem whilst trying to parse " + adfFileLocation, e);
                return false;
            } finally {
                if(watcher != null)
                    watcher.stopWatching();
            }


            if (listener != null) {
                listener.setProgress("Storing array design to DB");
            }

            // parsing completed, so now write the objects in the cache
            boolean result = writeObjects(cache);

            if (listener != null && result) {
                if (cache.fetchArrayDesignBundle() != null) {
                    listener.setAccession(cache.fetchArrayDesignBundle().getAccession());
                }
            }

            return true;
        }
        finally {
            AtlasLoadCacheRegistry.getRegistry().deregisterArrayDesign(arrayDesign);
            cache.clear();
        }
    }

    protected void configureHandlers() {
        HandlerPool pool = HandlerPool.getInstance();

        pool.replaceHandlerClass(AccessionHandler.class,
                                 AtlasLoadingAccessionHandler.class);
        pool.replaceHandlerClass(CompositeElementHandler.class,
                                 AtlasLoadingCompositeElementHandler.class);
        pool.replaceHandlerClass(ArrayDesignNameHandler.class,
                                 AtlasLoadingNameHandler.class);
        pool.replaceHandlerClass(ProviderHandler.class,
                                 AtlasLoadingProviderHandler.class);
        pool.replaceHandlerClass(TechnologyTypeHandler.class,
                                 AtlasLoadingTypeHandler.class);
    }

    protected boolean writeObjects(AtlasLoadCache cache) {
        int numOfObjects = cache.fetchArrayDesignBundle() == null ? 0 : 1;

        // validate the load(s)
        if (!validateLoad(cache.fetchArrayDesignBundle())) {
            return false;
        }

        // start the load(s)
        boolean success = false;
        startLoad(cache.fetchArrayDesignBundle().getAccession());

        try {
            // write the data
            getLog().info("Writing " + numOfObjects + " objects to Atlas 2 datasource...");

            long start, end;
            String total;

            // load array design bundles
            start = System.currentTimeMillis();
            getLog().info("Writing array design " + cache.fetchArrayDesignBundle().getAccession());
            // first, update the bundle with the identifier preferences
            cache.fetchArrayDesignBundle().setGeneIdentifierNamesInPriorityOrder(getGeneIdentifierPriority());

            getAtlasDAO().writeArrayDesignBundle(cache.fetchArrayDesignBundle());
            end = System.currentTimeMillis();
            total = new DecimalFormat("#.##").format((end - start) / 1000);
            getLog().info("Wrote array design {} in {}s.", cache.fetchArrayDesignBundle().getAccession(), total);

            // and return true - everything loaded ok
            getLog().info("Writing " + numOfObjects + " objects completed successfully");
            return success = true;
        }
        catch (Exception e) {
            getLog().error("Writing " + numOfObjects + " objects failed: " + e.getMessage() +
                    "\nData may be left in an inconsistent state: rerun this load to overwrite.", e);
            return success = false;
        }
        finally {
            // end the load(s)
            endLoad(cache.fetchArrayDesignBundle().getAccession(), success);
        }
    }

    private boolean validateLoad(ArrayDesignBundle arrayDesignBundle) {
        if (arrayDesignBundle == null) {
            getLog().error("No array design created - unable to load");
            return false;
        }

        if (!checkArrayDesign(arrayDesignBundle.getAccession())) {
            return false;
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
        getAtlasDAO().writeLoadDetails(accession,
                                       LoadStage.LOAD,
                                       LoadStatus.WORKING,
                                       LoadType.ARRAYDESIGN);
    }

    private void endLoad(String accession, boolean success) {
        getLog().info("Updating load_monitor: ending load for " + accession);
        getAtlasDAO().writeLoadDetails(accession,
                                       LoadStage.LOAD,
                                       success ? LoadStatus.DONE : LoadStatus.FAILED,
                                       LoadType.ARRAYDESIGN);
    }
}

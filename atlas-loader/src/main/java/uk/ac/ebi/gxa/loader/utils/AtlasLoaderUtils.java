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

package uk.ac.ebi.gxa.loader.utils;

import org.apache.commons.logging.Log;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.handler.Handler;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.lang.Progressible;
import uk.ac.ebi.arrayexpress2.magetab.lang.AbstractStatifiable;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.handler.sdrf.AtlasLoadingAssayHandler;
import uk.ac.ebi.gxa.loader.handler.sdrf.AtlasLoadingDerivedArrayDataMatrixHandler;
import uk.ac.ebi.gxa.loader.handler.sdrf.AtlasLoadingHybridizationHandler;
import uk.ac.ebi.gxa.loader.handler.sdrf.AtlasLoadingSourceHandler;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderService;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Set;

/**
 * Simple utilities classes dealing with common functions that are required in loading to the Atlas DB.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoaderUtils {
    /**
     * Blocking method that waits until an experiment with the given accession number is available.  Note that this
     * method will be interrupted if the investigation acquires a {@link uk.ac.ebi.arrayexpress2.magetab.lang.Status}<code>.FAILED</code>
     * status, but not otherwise.  If the calling code never writes an experiment with this accession into a cache
     * associated with the given investigation, this method will never terminate.
     *
     * @param accession     the accession of the experiment to wait for
     * @param investigation the investigation to wait on.  This investigation is used to retrieve an {@link
     *                      uk.ac.ebi.gxa.loader.cache.AtlasLoadCache} from the {@link uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry}
     *                      singleton.
     * @param handlerName   the name of the handler (or the client class) that is waiting - this is used for logging
     * @param log           a log stream to write - this is used in debug modes
     * @return true when the IDF has finished compiling, or false if it failed
     * @throws LookupException if lookup failed - for example, if the accession supplied was null, or if the
     *                         investigation acquires a failed status while this method was waiting.
     */
    public static Experiment waitForExperiment(String accession,
                                               final MAGETABInvestigation investigation,
                                               String handlerName,
                                               Log log) throws LookupException {
        // retrieve object bag
        AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);

        // check the identifier is not null
        if (accession == null) {
            throw new LookupException("Cannot lookup an object using a null accession");
        }

        log.debug(handlerName + " doing lookup for experiment " + accession);
        log.trace("Thread [" + Thread.currentThread().getName() + "] polling for dependent object");
        // fetch from the bag
        while (cache.fetchExperiment(accession) == null &&
                investigation.getStatus() != Status.COMPLETE &&
                investigation.getStatus() != Status.FAILED) {
            // object isn't in the bag yet, so wait
            synchronized (cache) {
                try {
                    log.trace("Thread [" + Thread.currentThread().getName() + "] waiting, no result yet");
                    // wait for new objects to be available
                    cache.wait(1000);
                    log.trace("Thread [" + Thread.currentThread().getName() + "] resumed");
                }
                catch (InterruptedException e) {
                    if (investigation.getStatus() == Status.FAILED) {
                        log.warn(handlerName + " was interrupted by a failure elsewhere " +
                                "whilst waiting for experiment " + accession + " and is terminating");
                        throw new LookupException(
                                "Interrupted by a fail whilst waiting " + "for experiment " + accession);
                    }
                    else {
                        // interrupted but no fail, so safe to continue
                    }
                }
            }
        }
        log.debug(handlerName + " resumed after dependent object obtained");
        return cache.fetchExperiment(accession);
    }

    /**
     * Blocking method that waits until an assay with the given accession number is available.  Note that this method
     * will be interrupted if the investigation acquires a {@link uk.ac.ebi.arrayexpress2.magetab.lang.Status}<code>.FAILED</code>
     * status, but not otherwise.  If the calling code never writes an assay with this accession into a cache associated
     * with the given investigation, this method will never terminate.
     *
     * @param accession     the accession of the assay to wait for
     * @param investigation the investigation to wait on.  This investigation is used to retrieve an {@link
     *                      uk.ac.ebi.gxa.loader.cache.AtlasLoadCache} from the {@link uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry}
     *                      singleton.
     * @param handlerName   the name of the handler (or the client class) that is waiting - this is used for logging
     * @param log           a log stream to write - this is used in debug modes
     * @return true when the IDF has finished compiling, or false if it failed
     * @throws LookupException if lookup failed - for example, if the accession supplied was null, or if the
     *                         investigation acquires a failed status while this method was waiting.
     */
    public static Assay waitForAssay(String accession,
                                     final MAGETABInvestigation investigation,
                                     String handlerName,
                                     Log log) throws LookupException {
        // retrieve object bag
        AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);

        // check the identifier is not null
        if (accession == null) {
            throw new LookupException("Cannot lookup an object using a null accession");
        }

        log.debug(handlerName + " doing lookup for assay " + accession + " in " + cache.toString());
        log.trace("Thread [" + Thread.currentThread().getName() + "] polling for assay");
        // fetch from the bag
        if (requiresWaiting("assayname") || requiresWaiting("hybridizationname")) {
            while (cache.fetchAssay(accession) == null &&
                    investigation.getStatus() != Status.COMPLETE &&
                    investigation.getStatus() != Status.FAILED) {
                // object isn't in the bag yet, so wait
                synchronized (cache) {
                    try {
                        log.trace("Thread [" + Thread.currentThread().getName() + "] waiting, no result yet");
                        // wait for new objects to be available
                        cache.wait(1000);
                        log.trace("Thread [" + Thread.currentThread().getName() + "] resumed");
                    }
                    catch (InterruptedException e) {
                        if (investigation.getStatus() == Status.FAILED) {
                            log.warn(handlerName + " was interrupted by a failure elsewhere " +
                                    "whilst waiting for assay " + accession + " and is terminating");
                            throw new LookupException(
                                    "Interrupted by a fail whilst waiting for assay " + accession);
                        }
                        else {
                            // interrupted but no fail, so safe to continue
                        }
                    }
                }
            }
            log.debug(handlerName + " resumed after dependent object obtained");
            return cache.fetchAssay(accession);
        }
        else {
            log.info(
                    "No loading handler registered for nodes of type 'assayname' or 'hybridizationname', so won't wait");
            return null;
        }
    }

    /**
     * Blocking method that waits until an sample with the given accession number is available.  Note that this method
     * will be interrupted if the investigation acquires a {@link uk.ac.ebi.arrayexpress2.magetab.lang.Status}<code>.FAILED</code>
     * status, but not otherwise.  If the calling code never writes an sample with this accession into a cache
     * associated with the given investigation, this method will never terminate.
     *
     * @param accession     the accession of the sample to wait for
     * @param investigation the investigation to wait on.  This investigation is used to retrieve an {@link
     *                      uk.ac.ebi.gxa.loader.cache.AtlasLoadCache} from the {@link uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry}
     *                      singleton.
     * @param handlerName   the name of the handler (or the client class) that is waiting - this is used for logging
     * @param log           a log stream to write - this is used in debug modes
     * @return true when the IDF has finished compiling, or false if it failed
     * @throws LookupException if lookup failed - for example, if the accession supplied was null, or if the
     *                         investigation acquires a failed status while this method was waiting.
     */
    public static Sample waitForSample(String accession,
                                       final MAGETABInvestigation investigation,
                                       String handlerName,
                                       Log log) throws LookupException {
        // retrieve object bag
        AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);

        // check the identifier is not null
        if (accession == null) {
            throw new LookupException("Cannot lookup an object using a null accession");
        }

        log.debug(handlerName + " doing lookup for sample " + accession);
        log.trace("Thread [" + Thread.currentThread().getName() + "] polling for sample");
        // fetch from the bag
        if (requiresWaiting("sourcename")) {
            while (cache.fetchSample(accession) == null &&
                    investigation.getStatus() != Status.COMPLETE &&
                    investigation.getStatus() != Status.FAILED) {
                // object isn't in the bag yet, so wait
                synchronized (cache) {
                    try {
                        log.trace("Thread [" + Thread.currentThread().getName() + "] waiting, no result yet");
                        // wait for new objects to be available
                        cache.wait(1000);
                        log.trace("Thread [" + Thread.currentThread().getName() + "] resumed");
                    }
                    catch (InterruptedException e) {
                        if (investigation.getStatus() == Status.FAILED) {
                            log.warn(handlerName + " was interrupted by a failure elsewhere " +
                                    "whilst waiting for sample " + accession + " and is terminating");
                            throw new LookupException("Interrupted by a fail whilst waiting for sample " + accession);
                        }
                        else {
                            // interrupted but no fail, so safe to continue
                        }
                    }
                }
            }
            log.debug(handlerName + " resumed after dependent object obtained");
            return cache.fetchSample(accession);
        }
        else {
            return null;
        }
    }

    public static String waitForArrayDesignAccession(MAGETABArrayDesign arrayDesign) throws LookupException {
        // need to have parsed the accession before we can do more
        while (arrayDesign.accession == null
                && arrayDesign.ADF.ranksBelow(Status.COMPILING)
                && arrayDesign.ADF.getStatus() != Status.FAILED) {
            synchronized (arrayDesign) {
                try {
                    arrayDesign.wait(1000);
                }
                catch (InterruptedException e) {
                    // ignore, check handled elsewhere
                }
            }
        }

        if (arrayDesign.accession == null) {
            throw new LookupException("Array Design reading completed, but no accession was parsed");
        }
        else {
            return arrayDesign.accession;
        }
    }

    public static ArrayDesignBundle waitForArrayDesignBundle(String accession,
                                                             MAGETABArrayDesign arrayDesign,
                                                             String handlerName,
                                                             Log log) throws LookupException {
        // retrieve object bag
        AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(arrayDesign);

        // check the identifier is not null
        if (accession == null) {
            throw new LookupException("Cannot lookup an object using a null accession");
        }

        log.debug(handlerName + " doing lookup for experiment " + accession);
        log.trace("Thread [" + Thread.currentThread().getName() + "] polling for dependent object");
        // fetch from the bag
        while (cache.fetchArrayDesignBundle(accession) == null &&
                arrayDesign.getStatus() != Status.COMPLETE &&
                arrayDesign.getStatus() != Status.FAILED) {
            // object isn't in the bag yet, so wait
            synchronized (cache) {
                try {
                    log.trace("Thread [" + Thread.currentThread().getName() + "] waiting, no result yet");
                    // wait for new objects to be available
                    cache.wait(1000);
                    log.trace("Thread [" + Thread.currentThread().getName() + "] resumed");
                }
                catch (InterruptedException e) {
                    if (arrayDesign.getStatus() == Status.FAILED) {
                        log.warn(handlerName + " was interrupted by a failure elsewhere " +
                                "whilst waiting for array design bundle " + accession + " and is terminating");
                        throw new LookupException(
                                "Interrupted by a fail whilst waiting " + " for array design bundle " + accession);
                    }
                    else {
                        // interrupted but no fail, so safe to continue
                    }
                }
            }
        }
        log.debug(handlerName + " resumed after dependent object obtained");
        return cache.fetchArrayDesignBundle(accession);

    }

    /**
     * Generates an accession number for the given {@link SDRFNode} in the given {@link
     * uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation}. This uses a simple concatenation of
     * investigation accession, followed by "::", followed by the node type, then "::" then the node name.
     *
     * @param investigation the investigation this node is present in.  If this investigation has no accession number,
     *                      "UNKNOWN" is used as the first part of the resulting accession
     * @param node          the node to generate the accession for
     * @return the accession that was generated
     */
    public static String getNodeAccession(MAGETABInvestigation investigation, SDRFNode node) {
        // no requirement to be unique, so just return node name
        return node.getNodeName();
    }

    public static boolean requiresWaiting(String nodeType) {
        Set<Class<? extends Handler>> handlerClasses = HandlerPool.getInstance().getHandlerClasses();
        if (nodeType.equals(new AtlasLoadingAssayHandler().handlesTag())) {
            // if nodeType is "assayname" check our pool contains AtlasLoadingAssayHandlers
            return (handlerClasses.contains(AtlasLoadingAssayHandler.class));
        }
        else if (nodeType.equals(new AtlasLoadingDerivedArrayDataMatrixHandler().handlesTag())) {
            // if nodeType is "derived array..." check our pool contains AtlasLoadingDerivedArrayDataMatrixHandlers
            return (handlerClasses.contains(AtlasLoadingDerivedArrayDataMatrixHandler.class));
        }
        else if (nodeType.equals(new AtlasLoadingHybridizationHandler().handlesTag())) {
            // if nodeType is "hyb..." check our pool contains AtlasLoadingHybridizationHandlers
            return (handlerClasses.contains(AtlasLoadingHybridizationHandler.class));
        }
        else if (nodeType.equals(new AtlasLoadingSourceHandler().handlesTag())) {
            // if nodeType is "sourcename" check our pool contains AtlasLoadingSourceHandlers
            return (handlerClasses.contains(AtlasLoadingSourceHandler.class));
        }
        else {
            return false;
        }
    }

    public abstract static class WatcherThread extends Thread {
        public abstract void stopWatching();
    }

    public static <T extends AbstractStatifiable & Progressible>
    WatcherThread createProgressWatcher(final T target, final AtlasLoaderService.Listener listener) {
        if(listener == null)
            return null;
        
        WatcherThread result = new WatcherThread() {
            private boolean running = true;

            public void stopWatching() {
                running = false;
                try {
                    join();
                } catch(InterruptedException e) {
                    // 
                }
            }

            @Override
            public void run() {
                while(running && target.getProgress() < 100 && target.getStatus() != Status.FAILED) {
                    listener.setProgress("Parsed " + target.getProgress() + "%");
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {
                        //
                    }
                }
                listener.setProgress("Parsed");
            }
        };
        result.start();
        return result;
    }
}

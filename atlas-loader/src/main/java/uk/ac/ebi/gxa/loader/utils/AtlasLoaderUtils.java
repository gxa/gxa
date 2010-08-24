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
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.lang.Progressible;
import uk.ac.ebi.arrayexpress2.magetab.lang.AbstractStatifiable;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Set;
import java.lang.ref.WeakReference;

/**
 * Simple utilities classes dealing with common functions that are required in loading to the Atlas DB.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoaderUtils {
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

    public abstract static class WatcherThread extends Thread {
        public abstract void stopWatching();
    }

    public static <T extends AbstractStatifiable & Progressible>
    WatcherThread createProgressWatcher(final T target, final AtlasLoaderServiceListener listener) {
        if(listener == null)
            return null;
        
        WatcherThread result = new WatcherThread() {
            private WeakReference<T> targetRef = new WeakReference<T>(target);
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
                while(running) {
                    T t = targetRef.get();
                    if(t == null || t.getStatus() == Status.FAILED)
                        break;

                    int progress = t.getProgress();
                    if(progress >= 100)
                        break;

                    listener.setProgress("Parsed " + progress + "%");
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

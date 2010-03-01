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

package uk.ac.ebi.gxa.loader.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.HashMap;
import java.util.Map;

/**
 * A singleton registry of AtlasLoadCache objects, indexed by MAGETABInvestigation.  This lets handlers in different
 * threads do a lookup on the cache to store objects in, and they only need to specifically know about the
 * MAGETABInvestigation.
 *
 * @author Tony Burdett
 * @date 27-Aug-2009
 */
public class AtlasLoadCacheRegistry {
    // singleton instance
    private static AtlasLoadCacheRegistry registry = new AtlasLoadCacheRegistry();

    /**
     * Obtain the singleton registry instance
     *
     * @return the AtlasLoadCache registry
     */
    public static AtlasLoadCacheRegistry getRegistry() {
        return registry;
    }

    private final Map<MAGETABInvestigation, AtlasLoadCache> investigationRegistry;
    private final Map<MAGETABArrayDesign, AtlasLoadCache> arrayRegistry;
    private final Log log = LogFactory.getLog(this.getClass().getSimpleName());

    /**
     * Private constructor for the registry
     */
    private AtlasLoadCacheRegistry() {
        this.investigationRegistry = new HashMap<MAGETABInvestigation, AtlasLoadCache>();
        this.arrayRegistry = new HashMap<MAGETABArrayDesign, AtlasLoadCache>();
    }

    /**
     * Register an {@link AtlasLoadCache}, keyed by investigation, to this registry.  Any objects created from this
     * investigation should be placed into this cache.
     * <p/>
     * Note that an IllegalArgumentException will be thrown if the investigation supplied is already associated with a
     * cache in this registry.  If you want to replace a cache, use the {@link #replaceExperiment(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation,
     * AtlasLoadCache)} method, or you can {@link #deregisterExperiment(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation)}
     * and then register.  Alternatively you can merge objects in a new cache into the one previously registered by
     * calling {@link #mergeExperiments(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation,
     * AtlasLoadCache)}.
     *
     * @param investigation the investigation being used to create objects for the cache
     * @param cache         the cache holding objects created from this investigation
     */
    public synchronized void registerExperiment(MAGETABInvestigation investigation,
                                                AtlasLoadCache cache) {
        log.info("Registering cache, and associating with an investigation");
        // register - but only if this investigation hasn't be registered before
        if (investigationRegistry.containsKey(investigation)) {
            throw new IllegalArgumentException(
                    "The supplied investigation has been previously registered");
        }
        else {
            investigationRegistry.put(investigation, cache);
        }
    }

    /**
     * Register an {@link AtlasLoadCache}, keyed by array design, to this registry.  Any objects created from this array
     * design should be placed into this cache.
     * <p/>
     * Note that an IllegalArgumentException will be thrown if the array design supplied is already associated with a
     * cache in this registry.  If you want to replace a cache, use the {@link #replaceArrayDesign(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign,
     * AtlasLoadCache)} method, or you can {@link #deregisterArrayDesign(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign)}
     * and then register.  Alternatively you can merge objects in a new cache into the one previously registered by
     * calling {@link #mergeArrayDesigns(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign,
     * AtlasLoadCache)}.
     *
     * @param arrayDesign the array design being used to create objects for the cache
     * @param cache       the cache holding objects created from this investigation
     */
    public synchronized void registerArrayDesign(MAGETABArrayDesign arrayDesign,
                                                 AtlasLoadCache cache) {
        log.info("Registering cache, and associating with an array design");
        // register - but only if this investigation hasn't be registered before
        if (arrayRegistry.containsKey(arrayDesign)) {
            throw new IllegalArgumentException(
                    "The supplied investigation has been previously registered");
        }
        else {
            arrayRegistry.put(arrayDesign, cache);
        }
    }

    /**
     * Deregisters the {@link AtlasLoadCache} keyed to this investigation from this registry.
     *
     * @param investigation the investigation that keys the registered cache of objects
     */
    public synchronized void deregisterExperiment(MAGETABInvestigation investigation) {
        log.info("Deregistering cache");

        // now register - but only if this investigation hasn't be registered before
        if (!investigationRegistry.containsKey(investigation)) {
            throw new IllegalArgumentException(
                    "The supplied investigation was never registered");
        }
        else {
            investigationRegistry.remove(investigation);
        }
    }

    /**
     * Deregisters the {@link AtlasLoadCache} keyed to this array design from this registry.
     *
     * @param arrayDesign the investigation that keys the registered cache of objects
     */
    public synchronized void deregisterArrayDesign(MAGETABArrayDesign arrayDesign) {
        log.info("Deregistering cache");

        // now register - but only if this investigation hasn't be registered before
        if (!arrayRegistry.containsKey(arrayDesign)) {
            throw new IllegalArgumentException(
                    "The supplied array was never registered");
        }
        else {
            arrayRegistry.remove(arrayDesign);
        }
    }

    /**
     * Replace the {@link AtlasLoadCache} that is currently registered to the given {@link
     * uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation} with the new one supplied.  This is equivalent to
     * calling {@link #deregisterExperiment(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation)} followed by
     * {@link #registerExperiment(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation, AtlasLoadCache)} }
     *
     * @param investigation the investigation keying the cache to replace
     * @param cache         the new cache that will replace the current cache
     */
    public synchronized void replaceExperiment(MAGETABInvestigation investigation,
                                               AtlasLoadCache cache) {
        deregisterExperiment(investigation);
        registerExperiment(investigation, cache);
    }

    /**
     * Replace the {@link AtlasLoadCache} that is currently registered to the given {@link
     * uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign} with the new one supplied.  This is equivalent to
     * calling {@link #deregisterArrayDesign(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign)} followed by
     * {@link #registerArrayDesign(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign, AtlasLoadCache)}
     *
     * @param arrayDesign the array design keying the cache to replace
     * @param cache       the new cache that will replace the current cache
     */
    public synchronized void replaceArrayDesign(MAGETABArrayDesign arrayDesign,
                                                AtlasLoadCache cache) {
        deregisterArrayDesign(arrayDesign);
        registerArrayDesign(arrayDesign, cache);
    }

    /**
     * Merges any objects in the given cache into the cache already registered to the given investigation
     *
     * @param investigation the investigation keying the cache of objects to merge
     * @param cache         the new cache, containing objects that will be merged into the existing cache
     */
    public synchronized void mergeExperiments(MAGETABInvestigation investigation,
                                              AtlasLoadCache cache) {
        if (!investigationRegistry.containsKey(investigation)) {
            throw new IllegalArgumentException(
                    "There is no cache registered to the supplied investigation");
        }
        else {
            AtlasLoadCache existingCache = investigationRegistry.get(investigation);

            for (Assay assay : cache.fetchAllAssays()) {
                existingCache.addAssay(assay);
            }
            for (Experiment experiment : cache.fetchAllExperiments()) {
                existingCache.addExperiment(experiment);
            }
            for (Sample sample : cache.fetchAllSamples()) {
                existingCache.addSample(sample);
            }
        }
    }

    /**
     * Merges any objects in the given cache into the cache already registered to the given investigation
     *
     * @param arrayDesign the investigation keying the cache of objects to merge
     * @param cache       the new cache, containing objects that will be merged into the existing cache
     */
    public synchronized void mergeArrayDesigns(MAGETABArrayDesign arrayDesign,
                                               AtlasLoadCache cache) {
        if (!arrayRegistry.containsKey(arrayDesign)) {
            throw new IllegalArgumentException(
                    "There is no cache registered to the supplied investigation");
        }
        else {
            AtlasLoadCache existingCache = arrayRegistry.get(arrayDesign);

            for (ArrayDesignBundle arrayDesignBundle : cache.fetchAllArrayDesignBundles()) {
                existingCache.addArrayDesignBundle(arrayDesignBundle);
            }
        }
    }

    /**
     * Lookup an {@link AtlasLoadCache} by the investigation being used.
     *
     * @param investigation the investigation being used to create objects
     * @return the cache linked to this investigation
     */
    public synchronized AtlasLoadCache retrieveAtlasLoadCache(
            MAGETABInvestigation investigation) {
        return investigationRegistry.get(investigation);
    }

    /**
     * Lookup an {@link AtlasLoadCache} by the array design being used.
     *
     * @param arrayDesign the investigation being used to create objects
     * @return the cache linked to this investigation
     */
    public synchronized AtlasLoadCache retrieveAtlasLoadCache(
            MAGETABArrayDesign arrayDesign) {
        return arrayRegistry.get(arrayDesign);
    }
}

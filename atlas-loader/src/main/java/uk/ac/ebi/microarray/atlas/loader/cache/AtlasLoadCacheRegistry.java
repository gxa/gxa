package uk.ac.ebi.microarray.atlas.loader.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.HashMap;
import java.util.Map;

/**
 * A singleton registry of AtlasLoadCache objects, indexed by
 * MAGETABInvestigation.  This lets handlers in different threads do a lookup on
 * the cache to store objects in, and they only need to specifically know about
 * the MAGETABInvestigation.
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

  private final Map<MAGETABInvestigation, AtlasLoadCache> cacheRegistry;
  private final Log log = LogFactory.getLog(this.getClass().getSimpleName());

  /**
   * Private constructor for the registry
   */
  private AtlasLoadCacheRegistry() {
    this.cacheRegistry = new HashMap<MAGETABInvestigation, AtlasLoadCache>();
  }

  /**
   * Register an {@link AtlasLoadCache}, keyed by investigation, to this
   * registry.  Any objects created from this investigation should be placed
   * into this cache.
   * <p/>
   * Note that an IllegalArgumentExcpetion will be thrown if the investigation
   * supplied is already associated with a cache in this registry.  If you want
   * to replace a cache, use the {@link #replace(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation,
   * AtlasLoadCache)} method, or you can {@link #deregister(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation)}
   * and then register.  Alternatively you can merge objects in a new cache into
   * the one previously registered by calling {@link #merge(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation,
   * AtlasLoadCache)}.
   *
   * @param investigation the investigation being used to create objects for the
   *                      cache
   * @param cache         the cache holding objects created from this
   *                      investigation
   */
  public synchronized void register(MAGETABInvestigation investigation,
                                    AtlasLoadCache cache) {
    log.info("Registering cache, and associating with an investigation");
    // register - but only if this investigation hasn't be registered before
    if (cacheRegistry.containsKey(investigation)) {
      throw new IllegalArgumentException(
          "The supplied investigation has been previously registered");
    }
    else {
      cacheRegistry.put(investigation, cache);
    }
  }

  /**
   * Deregisters the {@link uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCache}
   * keyed to this investigation from this registry.
   *
   * @param investigation the investigation that keys the registered cache of
   *                      objects
   */
  public synchronized void deregister(MAGETABInvestigation investigation) {
    log.info("Deregistering cache");

    // now register - but only if this investigation hasn't be registered before
    if (!cacheRegistry.containsKey(investigation)) {
      throw new IllegalArgumentException(
          "The supplied investigation was never registered");
    }
    else {
      cacheRegistry.remove(investigation);
    }
  }

  /**
   * Replace the {@link uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCache}
   * that is currently registered to the given {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation}
   * with the new one supplied.  This is equivalent to calling {@link
   * #deregister(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation)}
   * followed by {@link #register(uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation,
   * AtlasLoadCache)} }
   *
   * @param investigation the investigation keying the cache to replace
   * @param cache         the new cache that will replace the current cache
   */
  public synchronized void replace(MAGETABInvestigation investigation,
                                   AtlasLoadCache cache) {
    deregister(investigation);
    register(investigation, cache);
  }

  /**
   * Merges any objects in the given cache into the cache already registered to
   * the given investigation
   *
   * @param investigation the investigation keying the cache of objects to
   *                      merge
   * @param cache         the new cache, containing objects that will be merged
   *                      into the existing cache
   */
  public synchronized void merge(MAGETABInvestigation investigation,
                                 AtlasLoadCache cache) {
    if (!cacheRegistry.containsKey(investigation)) {
      throw new IllegalArgumentException(
          "There is no cache registered to the supplied investigation");
    }
    else {
      AtlasLoadCache existingCache = cacheRegistry.get(investigation);

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
   * Lookup an {@link AtlasLoadCache} by the investigation being used.
   *
   * @param investigation the investigation being used to create objects
   * @return the cache linked to this investigation
   */
  public AtlasLoadCache retrieveAtlasLoadCache(
      MAGETABInvestigation investigation) {
    return cacheRegistry.get(investigation);
  }
}

package uk.ac.ebi.microarray.atlas.loader.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;

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
   *
   * @param investigation the investigation being used to create objects for the
   *                      cache
   * @param cache         the cache holding objects created from this
   *                      investigation
   */
  public synchronized void register(MAGETABInvestigation investigation,
                                    AtlasLoadCache cache) {
    log.info("Registering cache, and associating with an investigation");
    // first, clear
    cache.clear();
    // now register
    cacheRegistry.put(investigation, cache);
  }

  public synchronized void deregister(MAGETABInvestigation investigation) {
    log.info("Deregistering cache");

    AtlasLoadCache cache = cacheRegistry.get(investigation);
    cache.clear();
    cacheRegistry.remove(investigation);
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

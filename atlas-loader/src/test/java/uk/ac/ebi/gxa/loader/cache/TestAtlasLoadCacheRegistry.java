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

package uk.ac.ebi.gxa.loader.cache;

import junit.framework.TestCase;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestAtlasLoadCacheRegistry extends TestCase {
  private MAGETABInvestigation investigation;
  private AtlasLoadCache cache;

  public void setUp() {
    // create an investigation
    investigation = new MAGETABInvestigation();
    cache = new AtlasLoadCache();
  }

  public void tearDown() {
    investigation = null;
    cache = null;
  }

  public void testRegisterAndRetrieve() {
    // attempt to register cache
    AtlasLoadCacheRegistry.getRegistry().registerExperiment(
        investigation,
        cache);

    // now check we can retrieve cache
    AtlasLoadCache fetched = AtlasLoadCacheRegistry.getRegistry()
        .retrieveAtlasLoadCache(investigation);

    assertSame("The fetched cache is not the same object as that " +
        "which was registered", cache, fetched);

    try {
      // try and register a different cache to the same investigation
      AtlasLoadCache cache2 = new AtlasLoadCache();
      AtlasLoadCacheRegistry.getRegistry().registerExperiment(
          investigation,
          cache2);

      // this should have thrown an exception - dupicate registering is illegal!
      fail();
    }
    catch (Exception e) {
      // correctly threw exception on duplicate registration
    }
  }

  public void testDeregister() {
    // attempt to register cache
    AtlasLoadCacheRegistry.getRegistry().registerExperiment(
        investigation,
        cache);

    // now deregister
    AtlasLoadCacheRegistry.getRegistry().deregisterExperiment(
        investigation);

    // check we can't retrieve the cache
    AtlasLoadCache fetched = AtlasLoadCacheRegistry.getRegistry()
        .retrieveAtlasLoadCache(investigation);

    assertNull("Fetched cache was not null after deregistering", fetched);
  }

  public void testReplace() {
    // attempt to register cache
    AtlasLoadCacheRegistry.getRegistry().registerExperiment(
        investigation,
        cache);

    try {
      // try and register a different cache to the same investigation
      AtlasLoadCache cache2 = new AtlasLoadCache();
      AtlasLoadCacheRegistry.getRegistry().replaceExperiment(
          investigation,
          cache2);

      // now check we can retrieve cache
      AtlasLoadCache fetched = AtlasLoadCacheRegistry.getRegistry()
          .retrieveAtlasLoadCache(investigation);

      // check the cache was replaced
      assertSame("cache was not replaced with cache2", fetched, cache2);
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

    // note - merging is deprecated 
//  public void testMerge() {
//    // attempt to register cache, no objects in this
//    AtlasLoadCacheRegistry.getRegistry().registerExperiment(
//        investigation,
//        cache);
//
//    assertNotNull("The fetched cache has an experiments",
//               cache.fetchExperiment().size(), 0);
//
//    try {
//      // try and register a different cache to the same investigation
//      AtlasLoadCache cache2 = new AtlasLoadCache();
//
//      Experiment exp = new Experiment();
//      exp.setAccession("TEST-EXP");
//      cache2.setExperiment(exp);
//
//      AtlasLoadCacheRegistry.getRegistry().mergeExperiments(
//          investigation,
//          cache2);
//
//      // now check we can retrieve cache
//      AtlasLoadCache fetched = AtlasLoadCacheRegistry.getRegistry()
//          .retrieveAtlasLoadCache(investigation);
//
//      // now check the cache we retrieve is cache, not cache 2
//      assertSame("The fetched cache is not the same as the " +
//          "originally registered cache", fetched, cache);
//      assertNotSame("The fetched cache is the same as the " +
//          "merged cache", fetched, cache2);
//
//      assertSame("Fetched cache contains wrong number of experiments",
//                 fetched.fetchExperiment().size(), 1);
//    }
//    catch (Exception e) {
//      e.printStackTrace();
//      fail();
//    }
//  }
}

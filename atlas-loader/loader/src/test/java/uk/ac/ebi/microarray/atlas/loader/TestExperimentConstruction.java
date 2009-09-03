package uk.ac.ebi.microarray.atlas.loader;

import junit.framework.TestCase;
import org.junit.Test;
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
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.AssayHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.HybridizationHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.SourceHandler;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCache;
import uk.ac.ebi.microarray.atlas.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.loader.handler.idf.AtlasLoadingAccessionHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.idf.AtlasLoadingInvestigationTitleHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.idf.AtlasLoadingPersonAffiliationHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.idf.AtlasLoadingPersonLastNameHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.sdrf.AtlasLoadingAssayHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.sdrf.AtlasLoadingHybridizationHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.sdrf.AtlasLoadingSourceHandler;
import uk.ac.ebi.microarray.atlas.loader.model.Experiment;
import uk.ac.ebi.microarray.atlas.loader.model.Sample;
import uk.ac.ebi.microarray.atlas.loader.model.Assay;
import uk.ac.ebi.microarray.atlas.loader.model.Property;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * todo: Javadocs go here!
 *
 * @author Tony Burdett
 * @date 27-Aug-2009
 */
public class TestExperimentConstruction extends TestCase {
  private static final String urlPath =
      "file:///home/tburdett/Documents/MAGE-TAB/E-MEXP-986/E-MEXP-986.idf.txt";

  private MAGETABInvestigation investigation;
  private AtlasLoadCache cache;

  private static URL parseURL;

  public void setUp() {
    // now, create an investigation
    investigation = new MAGETABInvestigation();
    cache = new AtlasLoadCache();

    AtlasLoadCacheRegistry.getRegistry().register(investigation, cache);

    try {
      parseURL = new URL(urlPath);
    }
    catch (MalformedURLException e) {
      fail();
    }
  }

  protected void tearDown() throws Exception {
    AtlasLoadCacheRegistry.getRegistry().deregister(investigation);
    investigation = null;
    cache = null;
  }

  @Test
  public void testReplaceHandlers() {
    HandlerPool pool = HandlerPool.getInstance();
    pool.replaceHandlerClass(AccessionHandler.class,
                             AtlasLoadingAccessionHandler.class);
    pool.replaceHandlerClass(InvestigationTitleHandler.class,
                             AtlasLoadingInvestigationTitleHandler.class);
    pool.replaceHandlerClass(PersonAffiliationHandler.class,
                             AtlasLoadingPersonAffiliationHandler.class);
    pool.replaceHandlerClass(PersonLastNameHandler.class,
                             AtlasLoadingPersonLastNameHandler.class);
  }

  public void testParseAndCheckExperiments() {
    HandlerPool pool = HandlerPool.getInstance();
    pool.replaceHandlerClass(AccessionHandler.class,
                             AtlasLoadingAccessionHandler.class);
    pool.replaceHandlerClass(InvestigationTitleHandler.class,
                             AtlasLoadingInvestigationTitleHandler.class);
    pool.replaceHandlerClass(PersonAffiliationHandler.class,
                             AtlasLoadingPersonAffiliationHandler.class);
    pool.replaceHandlerClass(PersonLastNameHandler.class,
                             AtlasLoadingPersonLastNameHandler.class);

    MAGETABParser parser = new MAGETABParser();
    parser.setParsingMode(ParserMode.READ_AND_WRITE);
    parser.addErrorItemListener(new ErrorItemListener() {

      public void errorOccurred(ErrorItem item) {
        System.err.println("Error: " + item.toString());
      }
    });

    try {
      parser.parse(parseURL, investigation);
    }
    catch (ParseException e) {
      e.printStackTrace();
      fail();
    }

    // parsing finished, look in our cache...
    assertEquals("Local cache doesn't contain only one experiment",
                 cache.fetchAllExperiments().size(), 1);

    assertEquals("Registered cache doesn't contain only one experiment",
                 AtlasLoadCacheRegistry.getRegistry()
                     .retrieveAtlasLoadCache(investigation)
                     .fetchAllExperiments().size(), 1);

    Experiment expt = cache.fetchExperiment("E-MEXP-986");
    assertNotNull("Experiment is null", expt);

//    System.out.println(expt.toString());
  }


  public void testParseAndCheckSamplesAndAssays() {
    HandlerPool pool = HandlerPool.getInstance();
    assertTrue(pool.replaceHandlerClass(SourceHandler.class,
                             AtlasLoadingSourceHandler.class));
    assertTrue(pool.replaceHandlerClass(AssayHandler.class,
                             AtlasLoadingAssayHandler.class));
    assertTrue(pool.replaceHandlerClass(HybridizationHandler.class,
                             AtlasLoadingHybridizationHandler.class));

    MAGETABParser parser = new MAGETABParser();
    parser.setParsingMode(ParserMode.READ_AND_WRITE);
    parser.addErrorItemListener(new ErrorItemListener() {

      public void errorOccurred(ErrorItem item) {
        System.err.println("Error: " + item.toString());
      }
    });

    try {
      parser.parse(parseURL, investigation);
    }
    catch (ParseException e) {
      e.printStackTrace();
      fail();
    }

    // parsing finished, look in our cache...
    assertNotSame("Local cache doesn't contain any samples",
                  cache.fetchAllSamples().size(), 0);

    assertNotSame("Registered cache doesn't contain any samples",
                  AtlasLoadCacheRegistry.getRegistry()
                      .retrieveAtlasLoadCache(investigation)
                      .fetchAllSamples().size(), 0);

    assertNotSame("Local cache doesn't contain any assays",
                  cache.fetchAllAssays().size(), 0);

    assertNotSame("Registered cache doesn't contain any assays",
                  AtlasLoadCacheRegistry.getRegistry()
                      .retrieveAtlasLoadCache(investigation)
                      .fetchAllAssays().size(), 0);

//    for (Sample s : cache.fetchAllSamples()) {
//      System.out.println(s.toString());
//      for (Property p : s.getProperties()) {
//        System.out.println(p.toString());
//      }
//    }
//
//    for (Assay a : cache.fetchAllAssays()) {
//      System.out.println(a.toString());
//      for (Property p : a.getProperties()) {
//        System.out.println(p.toString());
//      }
//    }
  }
}

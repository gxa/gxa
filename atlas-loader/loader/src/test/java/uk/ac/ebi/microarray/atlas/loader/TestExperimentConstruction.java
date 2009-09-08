package uk.ac.ebi.microarray.atlas.loader;

import junit.framework.TestCase;
import org.junit.Test;
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
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.AssayHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.DerivedArrayDataMatrixHandler;
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
import uk.ac.ebi.microarray.atlas.loader.handler.sdrf.AtlasLoadingDerivedArrayDataMatrixHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.sdrf.AtlasLoadingHybridizationHandler;
import uk.ac.ebi.microarray.atlas.loader.handler.sdrf.AtlasLoadingSourceHandler;
import uk.ac.ebi.microarray.atlas.loader.model.Assay;
import uk.ac.ebi.microarray.atlas.loader.model.Experiment;
import uk.ac.ebi.microarray.atlas.loader.model.ExpressionValue;

import java.net.URL;
import java.util.List;

public class TestExperimentConstruction extends TestCase {
  private MAGETABInvestigation investigation;
  private AtlasLoadCache cache;

  private URL parseURL;

  public void setUp() {
    // now, create an investigation
    investigation = new MAGETABInvestigation();
    cache = new AtlasLoadCache();

    AtlasLoadCacheRegistry.getRegistry().register(investigation, cache);

    parseURL = this.getClass().getClassLoader().getResource(
        "E-GEOD-3790.idf.txt");
  }

  protected void tearDown() throws Exception {
    AtlasLoadCacheRegistry.getRegistry().deregister(investigation);
    investigation = null;
    cache = null;
  }

  @Test
  public void testReplaceHandlers() {
    HandlerPool pool = HandlerPool.getInstance();
    assertTrue(pool.replaceHandlerClass(
        AccessionHandler.class,
        AtlasLoadingAccessionHandler.class));
    assertTrue(pool.replaceHandlerClass(
        InvestigationTitleHandler.class,
        AtlasLoadingInvestigationTitleHandler.class));
    assertTrue(pool.replaceHandlerClass(
        PersonAffiliationHandler.class,
        AtlasLoadingPersonAffiliationHandler.class));
    assertTrue(pool.replaceHandlerClass(
        PersonLastNameHandler.class,
        AtlasLoadingPersonLastNameHandler.class));
    assertTrue(pool.replaceHandlerClass(
        SourceHandler.class,
        AtlasLoadingSourceHandler.class));
    assertTrue(pool.replaceHandlerClass(
        AssayHandler.class,
        AtlasLoadingAssayHandler.class));
    assertTrue(pool.replaceHandlerClass(
        HybridizationHandler.class,
        AtlasLoadingHybridizationHandler.class));
    assertTrue(pool.replaceHandlerClass(
        DerivedArrayDataMatrixHandler.class,
        AtlasLoadingDerivedArrayDataMatrixHandler.class));
  }

  @Test
  public void testParseAndCheckExperiments() {
    HandlerPool pool = HandlerPool.getInstance();
    pool.useDefaultHandlers();
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
        // lookup message
        String message = "";
        for (ErrorCode ec : ErrorCode.values()) {
          if (item.getErrorCode() == ec.getIntegerValue()) {
            message = ec.getErrorMessage();
            break;
          }
        }
        if (message.equals("")) {
          message = "Unknown error";
        }

        // log the error
        System.err.println(
            "Parser reported:\n\t" +
                item.getErrorCode() + ": " + message + "\n\t\t- " +
                "occurred in parsing " + item.getParsedFile() + " " +
                "[line " + item.getLine() + ", column " + item.getCol() + "].");
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

    Experiment expt = cache.fetchExperiment("E-GEOD-3790");
    assertNotNull("Experiment is null", expt);
  }

  @Test
  public void testParseAndCheckSamplesAndAssays() {
    HandlerPool pool = HandlerPool.getInstance();
    pool.useDefaultHandlers();
    pool.replaceHandlerClass(SourceHandler.class,
                             AtlasLoadingSourceHandler.class);
    pool.replaceHandlerClass(AssayHandler.class,
                             AtlasLoadingAssayHandler.class);
    pool.replaceHandlerClass(HybridizationHandler.class,
                             AtlasLoadingHybridizationHandler.class);

    MAGETABParser parser = new MAGETABParser();
    parser.setParsingMode(ParserMode.READ_AND_WRITE);
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
          message = "Unknown error";
        }

        // log the error
        System.err.println(
            "Parser reported:\n\t" +
                item.getErrorCode() + ": " + message + "\n\t\t- " +
                "occurred in parsing " + item.getParsedFile() + " " +
                "[line " + item.getLine() + ", column " + item.getCol() + "].");
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
  }

  @Test
  public void testParseAndCheckExpressionValues() {
    HandlerPool pool = HandlerPool.getInstance();
    pool.useDefaultHandlers();
    pool.replaceHandlerClass(
        HybridizationHandler.class,
        AtlasLoadingHybridizationHandler.class);
    pool.replaceHandlerClass(
        AssayHandler.class,
        AtlasLoadingAssayHandler.class);
    pool.replaceHandlerClass(
        DerivedArrayDataMatrixHandler.class,
        AtlasLoadingDerivedArrayDataMatrixHandler.class);

    MAGETABParser parser = new MAGETABParser();
    parser.setParsingMode(ParserMode.READ_AND_WRITE);
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
          message = "Unknown error";
        }

        // log the error
        System.err.println(
            "Parser reported:\n\t" +
                item.getErrorCode() + ": " + message + "\n\t\t- " +
                "occurred in parsing " + item.getParsedFile() + " " +
                "[line " + item.getLine() + ", column " + item.getCol() + "].");
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
    boolean allEmpty = true;
    for (Assay assay : cache.fetchAllAssays()) {
      List<ExpressionValue> evs = assay.getExpressionValues();
      if (evs.size() > 0) {
        allEmpty = false;
        break;
      }
    }

    assertFalse("No expression values were read - all assays have 0 EVs",
                allEmpty);
  }
}

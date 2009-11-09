package uk.ac.ebi.gxa.loader.handler.idf;

import junit.framework.TestCase;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.AccessionHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonAffiliationHandler;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.net.URL;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestAtlasLoadingPersonAffiliationHandler extends TestCase {
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

    HandlerPool pool = HandlerPool.getInstance();
    pool.useDefaultHandlers();
    pool.replaceHandlerClass(
        PersonAffiliationHandler.class,
        AtlasLoadingPersonAffiliationHandler.class);

    // person affiliation is also dependent on experiments being created, so replace accession handler too
    pool.replaceHandlerClass(
        AccessionHandler.class,
        AtlasLoadingAccessionHandler.class);
  }

  public void tearDown() throws Exception {
    AtlasLoadCacheRegistry.getRegistry().deregister(investigation);
    investigation = null;
    cache = null;
  }

  public void testWriteValues() {
    // create a parser and invoke it - having replace the handle with the one we're testing, we should get one experiment in our load cache
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

        // log the error - but this isn't a fail on its own
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

    // get the title of the experiment
    String expected = "Cardiff University School of Medicine";
    String actual = "";
    for (Experiment exp : cache.fetchAllExperiments()) {
      actual = exp.getLab();
    }

    assertEquals("Labs don't match", expected, actual);
  }
}

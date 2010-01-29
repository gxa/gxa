package uk.ac.ebi.gxa.loader.handler.sdrf;

import junit.framework.TestCase;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.AccessionHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.DerivedArrayDataMatrixHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.HybridizationHandler;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.handler.idf.AtlasLoadingAccessionHandler;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestAtlasLoadingDerivedArrayDataMatrixHandler extends TestCase {
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
                DerivedArrayDataMatrixHandler.class,
                AtlasLoadingDerivedArrayDataMatrixHandler.class);

        assertTrue(pool.getHandlerClasses().contains(AtlasLoadingDerivedArrayDataMatrixHandler.class));

        // deata matrix is also dependent on experiments being created, so replace accession handler too
        pool.replaceHandlerClass(
                AccessionHandler.class,
                AtlasLoadingAccessionHandler.class);
        // deata matrix is also dependent on assays being created, so replace hyb handler too
        pool.replaceHandlerClass(
                HybridizationHandler.class,
                AtlasLoadingHybridizationHandler.class);
    }

    public void tearDown() throws Exception {
        AtlasLoadCacheRegistry.getRegistry().deregister(investigation);
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
                    // try and load from properties
                    try {
                        Properties props = new Properties();
                        Enumeration<URL> urls =
                                getClass().getClassLoader().getResources("META-INF/magetab/errorcodes.properties");
                        while (urls.hasMoreElements()) {
                            props.load(urls.nextElement().openStream());
                        }

                        String em = props.getProperty(Integer.toString(item.getErrorCode()));
                        if (em != null) {
                            message = em;
                        }
                        else {
                            message = "Unknown error";
                        }
                    }
                    catch (IOException e) {
                        message = "Unknown error";
                    }
                }

                // log the error - but this isn't a fail on its own
                System.err.println(
                        "Parser reported:\n\t" +
                                item.getErrorCode() + ": " + message + " (" +
                                item.getComment() + ")\n\t\t - " +
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

        System.out.println("Parsing done");

        // parsing finished, look in our cache...
        // expect 404 assays
        assertEquals("Local cache doesn't contain correct number of assays",
                     404, cache.fetchAllAssays().size());

        // get the title of the experiment
        for (Assay assay : cache.fetchAllAssays()) {
            Map<String, Float> evs = assay.getExpressionValuesByDesignElementReference();

            if (evs == null) {
                fail("Expression values are null for assay " + assay.getAccession());
            }
            System.out.println("Found " + evs.keySet().size() + " expression values for " +
                    assay.getAccession());
            assertNotSame("No evs found for assay " + assay.getAccession(),
                          evs.size(), 0);
        }
    }

    public void testFindUpstreamAssays() {
        // private method, test in context of writeValues()
    }

    public void testHasDataMatrixNodeAsChild() {
        // private method, test in context of writeValues()
    }
}

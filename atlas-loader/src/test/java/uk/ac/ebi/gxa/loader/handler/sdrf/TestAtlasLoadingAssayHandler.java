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
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.AssayHandler;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.handler.idf.AtlasLoadingAccessionHandler;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestAtlasLoadingAssayHandler extends TestCase {
    private MAGETABInvestigation investigation;
    private AtlasLoadCache cache;

    private URL parseURL;

    private volatile Integer counter;

    public void setUp() {
        // now, create an investigation
        investigation = new MAGETABInvestigation();
        cache = new AtlasLoadCache();

        AtlasLoadCacheRegistry.getRegistry().register(investigation, cache);

        parseURL = this.getClass().getClassLoader().getResource(
                "E-GEOD-3790B.idf.txt");

        counter = 0;

        HandlerPool pool = HandlerPool.getInstance();
        pool.useDefaultHandlers();
        pool.replaceHandlerClass(
                AssayHandler.class,
                AtlasLoadingAssayHandler.class);

        // person affiliation is also dependent on experiments being created, so replace accession handler too
        pool.replaceHandlerClass(
                AccessionHandler.class,
                AtlasLoadingAccessionHandler.class);
    }

    public void tearDown() throws Exception {
        AtlasLoadCacheRegistry.getRegistry().deregister(investigation);

        counter = null;
    }

    public void testWriteValues() {
        // create a parser and invoke it - having replace the handle with the one we're testing, we should get one experiment in our load cache
        MAGETABParser parser = new MAGETABParser();
        parser.setParsingMode(ParserMode.READ_AND_WRITE);
        parser.addErrorItemListener(new ErrorItemListener() {

            public void errorOccurred(ErrorItem item) {
                // update counter
                counter++;

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

        assertEquals("Should have rejected 404 assay to sample links, as samples aren't loaded", 404,
                     counter.intValue());

        // get the title of the experiment
        for (Assay assay : cache.fetchAllAssays()) {
            String acc = assay.getAccession();
            assertNotNull("Sample acc is null", acc);
        }

        // test properties of each assay
        for (Assay assay : cache.fetchAllAssays()) {
            List<Property> props = assay.getProperties();

            // should have one property, organism
            assertNotNull("Assay " + assay.getAccession() + " properties list is null", props);
            assertEquals("More than one property observed for assay " + assay.getAccession() +
                    ", should be Factor Value[Ecotype] only", 1, props.size());

            assertEquals("Property name is not 'Ecotype'", "Ecotype", props.get(0).getName());

            // test some property values at random
            if (assay.getAccession().equals("E-GEOD-3790::hybridizationname::11 CN A")) {
                assertEquals("Property value should be 'Cape Verde Islands'", "Cape Verde Islands",
                             props.get(0).getValue());
            }

            if (assay.getAccession().equals("E-GEOD-3790::hybridizationname::81 CB A")) {
                assertEquals("Property value should be 'Vancouver-0'", "Vancouver-0", props.get(0).getValue());
            }

            if (assay.getAccession().equals("E-GEOD-3790::hybridizationname::106 FC BA9  A")) {
                assertEquals("Property value should be 'Shahdara'", "Shahdara", props.get(0).getValue());
            }
        }
    }
}

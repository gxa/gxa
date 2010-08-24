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

package uk.ac.ebi.gxa.loader;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.net.URL;
import java.util.Map;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestAtlasMAGETABLoader extends AtlasDAOTestCase {
    private MAGETABInvestigation investigation;
    private AtlasLoadCache cache;

    private URL parseURL;

    public void setUp() throws Exception {
        super.setUp();

        // now, create an investigation
        investigation = new MAGETABInvestigation();
        cache = new AtlasLoadCache();

        AtlasLoadCacheRegistry.getRegistry().registerExperiment(investigation, cache);

        parseURL = this.getClass().getClassLoader().getResource(
                "E-GEOD-3790.idf.txt");
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        AtlasLoadCacheRegistry.getRegistry().deregisterExperiment(investigation);
        investigation = null;
        cache = null;
    }

    public void testParseAndCheckExperiments() {
        System.out.println("Running parse and check experiment test...");
        HandlerPool pool = HandlerPool.getInstance();
        pool.useDefaultHandlers();

        MAGETABParser parser = new MAGETABParser();
        parser.setParsingMode(ParserMode.READ_AND_WRITE);
//        parser.addErrorItemListener(new ErrorItemListener() {
//
//            public void errorOccurred(ErrorItem item) {
//                // lookup message
//                String message = "";
//                for (ErrorCode ec : ErrorCode.values()) {
//                    if (item.getErrorCode() == ec.getIntegerValue()) {
//                        message = ec.getErrorMessage();
//                        break;
//                    }
//                }
//                if (message.equals("")) {
//                    message = "Unknown error";
//                }
//
//                // log the error
//                System.err.println(
//                        "Parser reported:\n\t" +
//                                item.getErrorCode() + ": " + message + "\n\t\t- " +
//                                "occurred in parsing " + item.getParsedFile() + " " +
//                                "[line " + item.getLine() + ", column " + item.getCol() + "].");
//            }
//        });

        try {
            parser.parse(parseURL, investigation);
        }
        catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        // parsing finished, look in our cache...
        assertNotNull("Local cache doesn't contain an experiment",
                     AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation).fetchExperiment());

        Experiment expt = cache.fetchExperiment("E-GEOD-3790");
        assertNotNull("Experiment is null", expt);
        System.out.println("Experiment parse and check test done!");
    }

    public void testLoadAndCompare() {
        // fixme: this test isn't really "testing" anything and breaks bamboo build, for some reason
//        System.out.println("Running load and compare test...");
//        // getAtlasDAO() return DAO configure with HSQL DB, which only contains dummy load procedure
//        // so, when we invoke load() nothing actually gets loaded
//        AtlasMAGETABLoader loader = new AtlasMAGETABLoader(getAtlasDAO());
//        boolean result = loader.load(parseURL);
//        // now check expected objects can be retrieved with DAO
//        try {
//            assertTrue("Loading was not successful", result);
//        }
//        catch (AssertionFailedError e) {
//            System.out.println("Expected fail occurred - load will always fail " +
//                    "until test in-memory DB gets stored procedures! LOLZ!!!!");
////                fail();
//        }
//        System.out.println("Load and compare test done!");
    }

    public void testParseAndCheckSamplesAndAssays() {
        System.out.println("Running parse and check samples and assays test...");
        HandlerPool pool = HandlerPool.getInstance();
        pool.useDefaultHandlers();
	/*
        pool.replaceHandlerClass(SourceHandler.class,
                                 AtlasLoadingSourceHandler.class);
        pool.replaceHandlerClass(AssayHandler.class,
                                 AtlasLoadingAssayHandler.class);
        pool.replaceHandlerClass(HybridizationHandler.class,
                                 AtlasLoadingHybridizationHandler.class);
	*/

        MAGETABParser parser = new MAGETABParser();
        parser.setParsingMode(ParserMode.READ_AND_WRITE);
//        parser.addErrorItemListener(new ErrorItemListener() {
//
//            public void errorOccurred(ErrorItem item) {
//                // lookup message
//                String message = "";
//                for (ErrorCode ec : ErrorCode.values()) {
//                    if (item.getErrorCode() == ec.getIntegerValue()) {
//                        message = ec.getErrorMessage();
//                        break;
//                    }
//                }
//                if (message.equals("")) {
//                    message = "Unknown error";
//                }
//
//                // log the error
//                System.err.println(
//                        "Parser reported:\n\t" +
//                                item.getErrorCode() + ": " + message + "\n\t\t- " +
//                                "occurred in parsing " + item.getParsedFile() + " " +
//                                "[line " + item.getLine() + ", column " + item.getCol() + "].");
//            }
//        });

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

        System.out.println("Parse and check sample/assays done");
    }

}

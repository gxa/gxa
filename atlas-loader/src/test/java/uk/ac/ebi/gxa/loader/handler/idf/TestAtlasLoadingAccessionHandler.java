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

package uk.ac.ebi.gxa.loader.handler.idf;

import com.google.common.collect.HashMultimap;
import junit.framework.TestCase;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.ExperimentBuilder;
import uk.ac.ebi.gxa.loader.steps.CreateExperimentStep;
import uk.ac.ebi.gxa.loader.steps.ParsingStep;

import java.net.URL;


public class TestAtlasLoadingAccessionHandler extends TestCase {
    public static final Logger log = LoggerFactory.getLogger(TestAtlasLoadingAccessionHandler.class);

    private ExperimentBuilder cache;

    private URL parseURL;

    public static MAGETABInvestigation createParser(ExperimentBuilder cache, URL parseURL) throws AtlasLoaderException {
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

        final MAGETABInvestigation investigation = new ParsingStep().parse(parseURL);
        cache.setExperiment(new CreateExperimentStep().readExperiment(investigation, HashMultimap.<String, String>create()));
        return investigation;
    }

    public void setUp() {
        cache = new AtlasLoadCache();

        parseURL = this.getClass().getClassLoader().getResource(
                "E-GEOD-3790.idf.txt");

        HandlerPool pool = HandlerPool.getInstance();
        pool.useDefaultHandlers();
    }

    public void tearDown() throws Exception {
        cache = null;
    }

    public void testExperimentExists() throws AtlasLoaderException {
        // create a parser and invoke it - having replace the handle with the one we're testing, we should get one experiment in our load cache
        createParser(cache, parseURL);

        // parsing finished, look in our cache...
        assertNotNull("Local cache doesn't contain an experiment", cache.fetchExperiment());
    }


    public void testLoadingInvestigationTitle() throws AtlasLoaderException {
        createParser(cache, parseURL);
        // get the title of the experiment
        String expected =
                "Human cerebellum, frontal cortex [BA4, BA9] and caudate nucleus HD tissue experiment";
        String actual = cache.fetchExperiment().getDescription();

        assertEquals("Titles don't match", expected, actual);
    }


    public void testPersonAffiliation() throws AtlasLoaderException {
        createParser(cache, parseURL);

        // get the title of the experiment
        String expected = "Cardiff University School of Medicine";
        String actual = cache.fetchExperiment().getLab();

        assertEquals("Labs don't match", expected, actual);
    }


    public void testPersonLastName() throws AtlasLoaderException {
        createParser(cache, parseURL);

        // get the title of the experiment
        String expected = "Lesley Jones Angela Hodges";
        String actual = cache.fetchExperiment().getPerformer();

        assertEquals("Names don't match", expected, actual);
    }
}

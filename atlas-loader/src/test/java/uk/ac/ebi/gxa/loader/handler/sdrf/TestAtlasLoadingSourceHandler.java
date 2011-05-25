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

package uk.ac.ebi.gxa.loader.handler.sdrf;

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
import uk.ac.ebi.gxa.loader.MockFactory;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.loader.steps.CreateExperimentStep;
import uk.ac.ebi.gxa.loader.steps.ParsingStep;
import uk.ac.ebi.gxa.loader.steps.SourceStep;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

public class TestAtlasLoadingSourceHandler extends TestCase {
    public static final Logger log = LoggerFactory.getLogger(TestAtlasLoadingSourceHandler.class);

    private AtlasLoadCache cache;

    private URL parseURL;

    public void setUp() {
        cache = new AtlasLoadCache();

        parseURL = this.getClass().getClassLoader().getResource(
                "E-GEOD-3790.idf.txt");

        HandlerPool pool = HandlerPool.getInstance();
        pool.useDefaultHandlers();
    }

    public void testWriteValues() throws AtlasLoaderException {
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
                        } else {
                            message = "Unknown error";
                        }
                    } catch (IOException e) {
                        message = "Unknown error";
                    }
                }

                // log the error - but this isn't a fail on its own
                log.debug("Parser reported:\n\t" +
                        item.getErrorCode() + ": " + message + "\n\t\t- " +
                        "occurred in parsing " + item.getParsedFile() + " " +
                        "[line " + item.getLine() + ", column " + item.getCol() + "].");
            }
        });


        final MAGETABInvestigation investigation = new ParsingStep().parse(parseURL);
        cache.setExperiment(new CreateExperimentStep().readExperiment(investigation, HashMultimap.<String, String>create()));
        final LoaderDAO dao = MockFactory.createLoaderDAO();
        new SourceStep().readSamples(investigation, cache, dao);

        // parsing finished, look in our cache...
        // expect 404 samples
        assertEquals("Local cache doesn't contain correct number of samples",
                404, cache.fetchAllSamples().size());

        // get the title of the experiment
        for (Sample sample : cache.fetchAllSamples()) {
            String acc = sample.getAccession();
            log.debug("Next sample acc: " + acc);
            assertNotNull("Sample acc is null", acc);
        }
    }
}

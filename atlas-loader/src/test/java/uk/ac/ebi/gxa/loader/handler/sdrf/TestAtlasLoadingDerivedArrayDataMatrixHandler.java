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
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.listener.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.MockFactory;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.loader.steps.*;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

public class TestAtlasLoadingDerivedArrayDataMatrixHandler extends TestCase {
    private AtlasLoadCache cache;

    private URL parseURL;

    public void setUp() {
        cache = new AtlasLoadCache();
        cache.setAvailQTypes(
                Arrays.asList("AFFYMETRIX_VALUE,CHPSignal,rma_normalized,gcRMA,signal,value,quantification".toLowerCase().split(",")));

        parseURL = this.getClass().getClassLoader().getResource(
                "E-GEOD-3790.idf.txt");
    }

    public void testWriteValues() throws AtlasLoaderException {
        // create a parser and invoke it - having replace the handle with the one we're testing, we should get one experiment in our load cache
        MAGETABParser parser = new MAGETABParser();
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
                System.err.println(
                        "Parser reported:\n\t" +
                                item.getErrorCode() + ": " + message + " (" +
                                item.getComment() + ")\n\t\t - " +
                                "occurred in parsing " + item.getParsedFile() + " " +
                                "[line " + item.getLine() + ", column " + item.getCol() + "].");
            }
        });

        final MAGETABInvestigation investigation = new ParsingStep().parse(parseURL);
        cache.setExperiment(new CreateExperimentStep().readExperiment(investigation, HashMultimap.<String, String>create()));
        final LoaderDAO dao = MockFactory.createLoaderDAO();
        new SourceStep().readSamples(investigation, cache, dao);
        new AssayAndHybridizationStep().readAssays(investigation, cache, dao);
        new DerivedArrayDataMatrixStep().readProcessedData(investigation, cache);

        System.out.println("Parsing done");

        // parsing finished, look in our cache...
        // expect 404 assays
        assertEquals("Local cache doesn't contain correct number of assays",
                404, cache.fetchAllAssays().size());
    }

    public void testFindUpstreamAssays() {
        // private method, test in context of writeValues()
    }

    public void testHasDataMatrixNodeAsChild() {
        // private method, test in context of writeValues()
    }
}

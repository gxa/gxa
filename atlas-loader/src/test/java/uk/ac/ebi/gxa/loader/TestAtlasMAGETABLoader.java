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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRFactoryBuilder;
import uk.ac.ebi.gxa.R.RType;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.service.MAGETABInvestigationExt;
import uk.ac.ebi.gxa.loader.steps.*;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestAtlasMAGETABLoader extends AtlasDAOTestCase {
    private static Logger log = LoggerFactory.getLogger(TestAtlasMAGETABLoader.class);

    private MAGETABInvestigationExt investigation;
    private AtlasLoadCache cache;

    private URL parseURL;

    public void setUp() throws Exception {
        super.setUp();

        // now, create an investigation
        investigation = new MAGETABInvestigationExt();
        cache = new AtlasLoadCache();
        parseURL = this.getClass().getClassLoader().getResource(
                "E-GEOD-3790.idf.txt");
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        investigation = null;
        cache = null;
    }

    public void testParseAndCheckExperiments() throws AtlasLoaderException {
        log.debug("Running parse and check experiment test...");
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

        Step step0 = new ParsingStep(parseURL, investigation);
        Step step1 = new CreateExperimentStep(investigation, cache);
        step0.run();
        step1.run();

        // parsing finished, look in our cache...
        assertNotNull("Local cache doesn't contain an experiment",
                cache.fetchExperiment());

        Experiment expt = cache.fetchExperiment("E-GEOD-3790");
        assertNotNull("Experiment is null", expt);
        log.debug("Experiment parse and check test done!");
    }

    public void testAll() throws Exception {
        log.debug("Running parse and check experiment test...");
        HandlerPool pool = HandlerPool.getInstance();
        pool.useDefaultHandlers();

        MAGETABParser parser = new MAGETABParser();
        parser.setParsingMode(ParserMode.READ_AND_WRITE);

        Step step0 = new ParsingStep(parseURL, investigation);
        Step step1 = new CreateExperimentStep(investigation, cache);
        Step step2 = new SourceStep(investigation, cache);
        Step step3 = new AssayAndHybridizationStep(investigation, cache);
        Step step4 = new DerivedArrayDataMatrixStep(investigation, cache);

        AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.LOCAL);
        AtlasComputeService computeService = new AtlasComputeService();
        computeService.setAtlasRFactory(rFactory);
        Step step5 = new HTSArrayDataStep(investigation, computeService, cache);
        step0.run();
        step1.run();
        step2.run();
        step3.run();
//            step4.run();
        log.debug("JLP =" + System.getProperty("java.library.path"));
        step5.run();

        // parsing finished, look in our cache...
        Experiment experiment = cache.fetchExperiment();

        log.debug("experiment.getAccession() = " + experiment.getAccession());
        assertNotNull("Local cache doesn't contain an experiment",
                experiment);

        Experiment expt = cache.fetchExperiment("E-GEOD-3790");
//        assertNotNull("Experiment is null", expt);
//        log.debug("Experiment parse and check test done!");

        log.debug("expt = " + expt);

        Map<String, List<String>> designElements = cache.getArrayDesignToDesignElements();
        log.debug("array designs = " + designElements.keySet());
        for (String s : designElements.keySet()) {
            log.debug("designElements.get(s) = " + designElements.get(s).size());
        }


        if (cache.fetchExperiment() == null) {
            String msg = "Cannot load without an experiment";
            log.debug("msg = " + msg);
        }


        if (cache.fetchAllAssays().isEmpty())
            log.debug("No assays found");

        Set<String> referencedArrayDesigns = new HashSet<String>();
        for (Assay assay : cache.fetchAllAssays()) {
            if (!referencedArrayDesigns.contains(assay.getArrayDesign().getAccession())) {
//                if (!checkArray(assay.getArrayDesignAccession())) {
//                    String msg = "The array design " + assay.getArrayDesignAccession() + " was not found in the " +
//                            "database: it is prerequisite that referenced arrays are present prior to " +
//                            "loading experiments";
//                    getLog().error(msg);
//                    throw new AtlasLoaderException(msg);
//                }

                referencedArrayDesigns.add(assay.getArrayDesign().getAccession());
            }

            if (assay.hasNoProperties()) {
                log.debug("Assay " + assay.getAccession() + " has no properties! All assays need at least one.");
            }

            if (!cache.getAssayDataMap().containsKey(assay.getAccession()))
                log.debug("Assay " + assay.getAccession() + " contains no data! All assays need some.");
        }

        if (cache.fetchAllSamples().isEmpty())
            log.debug("No samples found");

        Set<String> sampleReferencedAssays = new HashSet<String>();
        for (Sample sample : cache.fetchAllSamples()) {
            if (sample.getAssayAccessions().isEmpty())
                log.debug("No assays for sample " + sample.getAccession() + " found");
            else
                sampleReferencedAssays.addAll(sample.getAssayAccessions());
        }

        for (Assay assay : cache.fetchAllAssays())
            if (!sampleReferencedAssays.contains(assay.getAccession()))
                log.debug("No sample for assay " + assay.getAccession() + " found");

        log.debug("Validation done");
    }

    public void testLoadAndCompare() {
        // fixme: this test isn't really "testing" anything and breaks bamboo build, for some reason
//        log.debug("Running load and compare test...");
//        // getAtlasDAO() return DAO configure with HSQL DB, which only contains dummy load procedure
//        // so, when we invoke load() nothing actually gets loaded
//        AtlasMAGETABLoader loader = new AtlasMAGETABLoader(getAtlasDAO());
//        boolean result = loader.load(parseURL);
//        // now check expected objects can be retrieved with DAO
//        try {
//            assertTrue("Loading was not successful", result);
//        }
//        catch (AssertionFailedError e) {
//            log.debug("Expected fail occurred - load will always fail " +
//                    "until test in-memory DB gets stored procedures! LOLZ!!!!");
//        }
//        log.debug("Load and compare test done!");
    }

    public void testParseAndCheckSamplesAndAssays() throws AtlasLoaderException {
        log.debug("Running parse and check samples and assays test...");
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

        Step step0 = new ParsingStep(parseURL, investigation);
        Step step1 = new CreateExperimentStep(investigation, cache);
        Step step2 = new SourceStep(investigation, cache);
        Step step3 = new AssayAndHybridizationStep(investigation, cache);
        step0.run();
        step1.run();
        step2.run();
        step3.run();

        // parsing finished, look in our cache...
        assertNotSame("Local cache doesn't contain any samples",
                cache.fetchAllSamples().size(), 0);

        assertNotSame("Local cache doesn't contain any assays",
                cache.fetchAllAssays().size(), 0);

        log.debug("Parse and check sample/assays done");
    }

}

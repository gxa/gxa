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

import java.net.URL;
import java.util.HashSet;
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

        Step step0 = new ParsingStep(parseURL, investigation);
        Step step1 = new CreateExperimentStep(investigation, cache);
        step0.run();
        step1.run();

        // parsing finished, look in our cache...
        Experiment expt = cache.fetchExperiment();
        assertNotNull("Local cache doesn't contain an experiment", expt);
        assertEquals("Experiment is null", "E-GEOD-3790", expt.getAccession());
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

        AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.LOCAL);
        AtlasComputeService computeService = new AtlasComputeService();
        computeService.setAtlasRFactory(rFactory);
        Step step5 = new HTSArrayDataStep(investigation, computeService, cache);
        step0.run();
        step1.run();
        step2.run();
        step3.run();
        log.debug("JLP =" + System.getProperty("java.library.path"));
        step5.run();

        // parsing finished, look in our cache...
        Experiment experiment = cache.fetchExperiment();

        log.debug("experiment.getAccession() = " + experiment.getAccession());
        assertNotNull("Local cache doesn't contain an experiment",
                experiment);

        Experiment expt = cache.fetchExperiment();
        assertNotNull("Experiment is null", expt);
        assertEquals("Wrong experiment", "E-GEOD-3790", expt.getAccession());

        Set<String> referencedArrayDesigns = new HashSet<String>();
        for (Assay assay : cache.fetchAllAssays()) {
            if (!referencedArrayDesigns.contains(assay.getArrayDesign().getAccession())) {
                referencedArrayDesigns.add(assay.getArrayDesign().getAccession());
            }
        }
    }

    public void testParseAndCheckSamplesAndAssays() throws AtlasLoaderException {
        log.debug("Running parse and check samples and assays test...");
        HandlerPool pool = HandlerPool.getInstance();
        pool.useDefaultHandlers();

        MAGETABParser parser = new MAGETABParser();
        parser.setParsingMode(ParserMode.READ_AND_WRITE);


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

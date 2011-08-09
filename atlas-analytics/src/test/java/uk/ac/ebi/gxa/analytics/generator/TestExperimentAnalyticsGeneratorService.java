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

package uk.ac.ebi.gxa.analytics.generator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRFactoryBuilder;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGenerationEvent;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.analytics.generator.service.ExperimentAnalyticsGeneratorService;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestExperimentAnalyticsGeneratorService extends AtlasDAOTestCase {
    private final static String E_GEOD_5035 = "E-GEOD-5035";
    private final static String A_AFFY_45 = "A-AFFY-45";

    private AtlasDataDAO atlasDataDAO;
    private ExperimentAnalyticsGeneratorService experimentAnalyticsGeneratorService;
    private AtlasComputeService atlasComputeService;
    private ExecutorService ste;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        atlasDataDAO = new AtlasDataDAO();
        atlasDataDAO.setAtlasDataRepo(new File(getClass().getClassLoader().getResource("").getPath()));

        AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory();
        // build service
        atlasComputeService = new AtlasComputeService();
        atlasComputeService.setAtlasRFactory(rFactory);

        ste = Executors.newSingleThreadExecutor();

        experimentAnalyticsGeneratorService =
                new ExperimentAnalyticsGeneratorService(atlasDAO, atlasDataDAO, atlasComputeService, ste);
    }

    @After
    @Override
    public void tearDown() {
        atlasComputeService.shutdown();
        ste.shutdownNow();
    }

    @Test
    public void testGetRCodeFromResource() throws IOException {
        // open a stream to the resource
        InputStream in = getClass().getClassLoader().getResourceAsStream("R/analytics.R");

        // create a reader to read in code
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
    }

    @Test
    public void testCreateAnalyticsForExperimentWithoutFactors () throws AnalyticsGeneratorException {
        TestingAnalyticsGeneratorListener agl = new TestingAnalyticsGeneratorListener();
        experimentAnalyticsGeneratorService.createAnalyticsForExperiment(E_GEOD_5035, agl);
        assertTrue(agl.warnings.contains("No analytics were computed for " + E_GEOD_5035 + "/" +
                            A_AFFY_45 + " as it contained no factors!"));
    }

    private class TestingAnalyticsGeneratorListener implements AnalyticsGeneratorListener {
        public boolean wasSuccessful = false;
        public Set<String> progresses = new HashSet<String>();
        public Set<String> warnings   = new HashSet<String>();

        @Override
        public void buildSuccess() {
            wasSuccessful = true;
        }

        @Override
        public void buildError(AnalyticsGenerationEvent event) {
            // ignore
        }

        @Override
        public void buildProgress(String progressStatus) {
            progresses.add(progressStatus);
        }

        @Override
        public void buildWarning(String message) {
            warnings.add(message);
        }
    }
}

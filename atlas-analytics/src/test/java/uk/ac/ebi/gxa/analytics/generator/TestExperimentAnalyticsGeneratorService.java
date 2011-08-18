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

import junit.framework.AssertionFailedError;
import org.easymock.EasyMock;
import org.junit.Test;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.analytics.generator.listener.AnalyticsGeneratorListener;
import uk.ac.ebi.gxa.analytics.generator.service.ExperimentAnalyticsGeneratorService;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.data.AtlasDataDAO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class TestExperimentAnalyticsGeneratorService extends AtlasDAOTestCase {
    private final static String E_GEOD_5035 = "E-GEOD-5035";

    @Test
    public void testGetRCodeFromResource() throws IOException {
        // open a stream to the resource
        InputStream in = getClass().getClassLoader().getResourceAsStream("R/analytics.R");
        assertNotNull(in);
        in.close();
    }

    @Test
    public void testCreateAnalyticsForExperimentWithoutFactors () throws AnalyticsGeneratorException {
        final AtlasDataDAO atlasDataDAO = new AtlasDataDAO();
        atlasDataDAO.setAtlasDataRepo(new File(getClass().getClassLoader().getResource("").getPath()));

        final AtlasComputeService atlasComputeService = createMock(AtlasComputeService.class);
        expect(atlasComputeService
                .computeTask(EasyMock.<ComputeTask<Object>>anyObject()))
                .andThrow(new AssertionFailedError("Unexpected call to computeTask"))
                .anyTimes();
        replay(atlasComputeService);

        final ExperimentAnalyticsGeneratorService experimentAnalyticsGeneratorService =
                new ExperimentAnalyticsGeneratorService(
                atlasDAO, atlasDataDAO, atlasComputeService,
                createMock(ExecutorService.class));

        experimentAnalyticsGeneratorService.createAnalyticsForExperiment(E_GEOD_5035,
                createMock(AnalyticsGeneratorListener.class));
    }
}

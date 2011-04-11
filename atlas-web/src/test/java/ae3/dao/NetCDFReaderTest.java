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

package ae3.dao;

import ae3.model.ExperimentalData;
import org.junit.After;
import org.junit.Test;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.microarray.atlas.model.ExperimentImpl;
import uk.ac.ebi.microarray.atlas.services.ExperimentDAO;
import uk.ac.ebi.gxa.Experiment;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertNotNull;

/**
 * @author pashky
 */
public class NetCDFReaderTest {

    @Test
    public void testLoadExperiment() throws IOException, URISyntaxException {
        final Experiment experiment = createExperiment("E-MEXP-1586", 1036805754L);

        AtlasNetCDFDAO dao = new AtlasNetCDFDAO();
        dao.setAtlasDataRepo(getTestNCDir());
        dao.setExperimentDAO(createExperimentDAO(experiment));
        // /atlas-web/target/test-classes/MEXP/1500/E-MEXP-1586/1036805754_160588088.nc
        ExperimentalData expData = NetCDFReader.loadExperiment(dao, experiment.getAccession());
        assertNotNull(expData);
        assertEquals(1, expData.getArrayDesigns().size());

        assertEquals(0, expData.getExpressionsForGene(123456).size());
        assertEquals(expData.getAssays().size(), expData.getExpressionsForGene(160588339).size());
    }

    @Test
    public void testMultiArrayDesign() throws IOException, URISyntaxException {
        final Experiment experiment = createExperiment("E-MEXP-1913", 1036804993L);

        AtlasNetCDFDAO dao = new AtlasNetCDFDAO();
        dao.setAtlasDataRepo(getTestNCDir());
        dao.setExperimentDAO(createExperimentDAO(experiment));
        // /atlas-web/target/test-classes/MEXP/1900/E-MEXP-1913/1036804668_153069949.nc
        // /atlas-web/target/test-classes/MEXP/1900/E-MEXP-1913/1036804668_165554923.nc
        ExperimentalData expData = NetCDFReader.loadExperiment(dao, experiment.getAccession());
        assertNotNull(expData);
        assertEquals(2, expData.getArrayDesigns().size());

        assertEquals(0, expData.getExpressionsForGene(123456).size());
        assertTrue(expData.getAssays().size() > expData.getExpressionsForGene(102013).size());
        assertTrue(expData.getAssays().size() > expData.getExpressionsForGene(160591550).size());
    }

    private static File getTestNCDir() throws URISyntaxException {
        // won't work for JARs, networks and stuff, but so far so good...
        return new File(NetCDFReaderTest.class.getClassLoader().getResource("").getPath());
    }

    @After
    public void cleanup() {
        ResourceWatchdogFilter.cleanup();
    }


    private ExperimentDAO createExperimentDAO(Experiment experiment) {
        final ExperimentDAO experimentDAO = createMock(ExperimentDAO.class);
        expect(experimentDAO.getExperimentByAccession(experiment.getAccession())).andReturn(experiment).anyTimes();
        replay(experimentDAO);
        return experimentDAO;
    }

    private static Experiment createExperiment(String accession, long id) {
        return ExperimentImpl.create(accession, id);
    }

}

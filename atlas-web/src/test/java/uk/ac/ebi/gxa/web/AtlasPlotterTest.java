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

package uk.ac.ebi.gxa.web;

import ae3.dao.GeneSolrDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.AbstractIndexDataTestCase;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Tony Burdett
 */
public class AtlasPlotterTest extends AbstractIndexDataTestCase {
    private AtlasPlotter plotter;
    private GeneSolrDAO geneSolrDAO;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        geneSolrDAO = new GeneSolrDAO();
        geneSolrDAO.setGeneSolr(getSolrServerAtlas());

        plotter = new AtlasPlotter();
        plotter.setAtlasDatabaseDAO(atlasDAO);
        plotter.setGeneSolrDAO(getAtlasSolrDao());
        plotter.setAtlasDataDAO(getDataDAO());
    }

    @After
    public void tearDown() throws Exception {
        plotter = null;
        super.tearDown();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    public void testGetGeneInExpPlotData() throws Exception {
        final String geneid = getDataSet().getTable("A2_BIOENTITY").getValue(0, "BIOENTITYID").toString();

        Experiment experiment = atlasDAO.getExperimentByAccession(getDataSet().getTable("A2_EXPERIMENT").getValue(0, "accession").toString());

        List<Assay> assays = experiment.getAssays();

        final AssayProperty property = assays.get(0).getProperties("cell_type").iterator().next();
        final String ef = property.getName();
        final String efv = property.getValue();

        Map<String, Object> plot = plotter.getGeneInExpPlotData(geneid, experiment, ef, efv, "thumb");
        assertNotNull("Plot object was not constructed", plot);

        @SuppressWarnings("unchecked")
        Map<String, Object> series = (Map<String, Object>) (((List) plot.get("series")).get(0));
        assertNotNull("Data was not retrieved for plotting", series);

        ArrayList data = (ArrayList) series.get("data");
        assertTrue("Data retrieved was empty", data.size() > 0);
    }

    public GeneSolrDAO getAtlasSolrDao() {
        return geneSolrDAO;
    }
}

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

import ae3.dao.AtlasSolrDAO;
import uk.ac.ebi.gxa.AbstractIndexNetCDFTestCase;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.services.ExperimentDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;

/**
* @author Tony Burdett
*/
public class AtlasPlotterTest extends AbstractIndexNetCDFTestCase {
    private AtlasPlotter plotter;
    private AtlasSolrDAO atlasSolrDAO;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        atlasSolrDAO = new AtlasSolrDAO();
        atlasSolrDAO.setSolrServerAtlas(getSolrServerAtlas());
        atlasSolrDAO.setSolrServerExpt(getSolrServerExpt());

        plotter = new AtlasPlotter();
        plotter.setAtlasDatabaseDAO(getAtlasDAO());
        plotter.setAtlasSolrDAO(getAtlasSolrDao());
        plotter.setAtlasNetCDFDAO(getNetCDFDAO());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        plotter = null;
    }

    public void testGetGeneInExpPlotData() throws Exception {
        final String geneid = getDataSet().getTable("A2_GENE").getValue(0, "geneid").toString();

        Experiment experiment = new Experiment();
        experiment.setExperimentID(Long.parseLong(getDataSet().getTable("A2_EXPERIMENT").getValue(0, "experimentid").toString()));
        experiment.setAccession(getDataSet().getTable("A2_EXPERIMENT").getValue(0, "accession").toString());
        getNetCDFDAO().setExperimentDAO(createExperimentDAO(experiment));


        List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(experiment.getAccession());
        final Property property = assays.get(0).getProperties("cell_type").get(0);
        final String ef = property.getName();
        final String efv = property.getValue();

        Map<String, Object> plot = plotter.getGeneInExpPlotData(geneid, experiment.getAccession(), ef, efv, "thumb");
        assertNotNull("Plot object was not constructed", plot);

        @SuppressWarnings("unchecked")
        Map<String, Object> series = (Map<String, Object>) (((List) plot.get("series")).get(0));
        assertNotNull("Data was not retrieved for plotting", series);

        ArrayList data = (ArrayList) series.get("data");
        assertTrue("Data retrieved was empty", data.size() > 0);
    }

    public AtlasSolrDAO getAtlasSolrDao() {
        return atlasSolrDAO;
    }


    private ExperimentDAO createExperimentDAO(Experiment experiment) {
        final ExperimentDAO experimentDAO = createMock(ExperimentDAO.class);
        expect(experimentDAO.getExperimentByAccession(experiment.getAccession())).andReturn(experiment).anyTimes();
        replay(experimentDAO);
        return experimentDAO;
    }
}
package uk.ac.ebi.gxa.web;

import ae3.dao.AtlasDao;
import uk.ac.ebi.gxa.AbstractIndexNetCDFTestCase;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 12-Nov-2009
 */
public class TestAtlasPlotter extends AbstractIndexNetCDFTestCase {
    private AtlasPlotter plotter;
    private AtlasDao atlasSolrDao;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        atlasSolrDao = new AtlasDao();
        atlasSolrDao.setSolrServerAtlas(getSolrServerAtlas());
        atlasSolrDao.setSolrServerExpt(getSolrServerExpt());

        plotter = new AtlasPlotter();
        plotter.setAtlasDatabaseDAO(getAtlasDAO());
        plotter.setAtlasSolrDAO(getAtlasSolrDao());
        plotter.setAtlasNetCDFRepo(getNetCDFRepoLocation());
    }

    @Override
    protected void tearDown() throws Exception {
        plotter = null;
    }

    public void testGetGeneInExpPlotData() {
        try {
            final String geneid = getDataSet().getTable("A2_GENE").getValue(0, "geneid").toString();
            final String exptid = getDataSet().getTable("A2_EXPERIMENT").getValue(0, "experimentid").toString();
            final String accession = getDataSet().getTable("A2_EXPERIMENT").getValue(0, "accession").toString();

            List<Assay> assays = getAtlasDAO().getAssaysByExperimentAccession(accession);
            final String ef = assays.get(0).getProperties().get(0).getName();
            final String efv = assays.get(0).getProperties().get(0).getValue();

            Map<String,Object> plot = plotter.getGeneInExpPlotData(geneid, exptid, ef, efv, "thumb");
            assertNotNull("Plot object was not constructed", plot);

            Map<String,Object> series = (Map<String,Object>) (((List) plot.get("series")).get(0));
            assertNotNull("Data was not retrieved for plotting", series);

            ArrayList data = (ArrayList) series.get("data");
            assertTrue("Data retrieved was empty", data.size() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public AtlasDao getAtlasSolrDao() {
        return atlasSolrDao;
    }
}

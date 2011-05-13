package uk.ac.ebi.gxa.netcdf.reader;

import com.google.common.base.Predicates;
import junit.framework.TestCase;
import uk.ac.ebi.gxa.Model;
import uk.ac.ebi.gxa.impl.ModelImpl;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.*;

/**
 * This class tests functionality of AtlasNetCDFDAO
 *
 * @author Robert Petryszak
 */
public class TestNetCDFDAO extends TestCase {
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private Long geneId;
    private Experiment experiment;
    private String ef;
    private String efv;
    private float minPValue;
    private String designElementAccessionForMinPValue;
    private Set<Long> geneIds;
    private String proxyId;
    private final static DecimalFormat pValFormat = new DecimalFormat("0.#######");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        geneId = 153070209l; // human brca1
        proxyId = "411512559_153069949.nc";
        ef = "cell_type";
        efv = "germ cell";
        minPValue = 0.9999986f;
        designElementAccessionForMinPValue = "204531_s_at";

        experiment = new ModelImpl().createExperiment(411512559L, "E-MTAB-25");

        final Model model = createMock(Model.class);
        expect(model.getExperimentByAccession(experiment.getAccession())).andReturn(experiment).anyTimes();

        replay(model);

        atlasNetCDFDAO = new AtlasNetCDFDAO();
        atlasNetCDFDAO.setAtlasDataRepo(new File(getClass().getClassLoader().getResource("").getPath()));

        atlasNetCDFDAO.setAtlasModel(model);
        geneIds = new HashSet<Long>();
        geneIds.add(geneId);
    }

    public void testGetFactorValues() throws IOException {
        List<String> fvs = atlasNetCDFDAO.getFactorValues(experiment.getAccession(), proxyId, ef);
        assertNotNull(fvs);
        assertNotSame(fvs.size(), 0);
        assertTrue(fvs.contains(efv));
    }

    public void testGetExpressionAnalyticsByGeneID() throws IOException {
        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                atlasNetCDFDAO.getExpressionAnalysesForGeneIds(experiment.getAccession(), geneIds,
                        Predicates.<NetCDFProxy>alwaysTrue());

        // check the returned data
        assertNotNull(geneIdsToEfToEfvToEA.get(geneId));
        assertNotNull(geneIdsToEfToEfvToEA.get(geneId).get(ef));
        ExpressionAnalysis ea = geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv);

        assertNotNull(ea);
        assertNotNull("Got null for design element ID", ea.getDesignElementAccession());
        assertNotNull("Got null for experiment ID", ea.getExperimentID());
        assertNotNull("Got null for ef name", ea.getEfName());
        assertNotNull("Got null for efv name", ea.getEfvName());
        assertNotNull("Got null for ef id", ea.getEfId());
        assertNotNull("Got null for efv id", ea.getEfvId());
        assertNotNull("Got null for pvalue", ea.getPValAdjusted());
        assertNotNull("Got null for tstat", ea.getTStatistic());
        assertNotNull("Got null for proxyid", ea.getProxyId());
        assertNotNull("Got null for design element index", ea.getDesignElementIndex());
        System.out.println("Got expression analysis for gene id: " + geneId + " \n" + ea.toString());


        assertEquals(designElementAccessionForMinPValue, ea.getDesignElementAccession());
        assertEquals(pValFormat.format(minPValue), pValFormat.format(ea.getPValAdjusted()));
    }
}

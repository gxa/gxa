package uk.ac.ebi.gxa.netcdf.reader;

import com.google.common.base.Predicates;
import junit.framework.TestCase;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.services.ExperimentDAO;

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
    private Long designElementIdForMinPValue;
    Set<Long> geneIds;
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
        designElementIdForMinPValue = 153085549l;

        experiment = new Experiment();
        experiment.setAccession("E-MTAB-25");
        experiment.setExperimentID(411512559L);

        final ExperimentDAO experimentDAO = createMock(ExperimentDAO.class);
        expect(experimentDAO.getExperimentByAccession(experiment.getAccession())).andReturn(experiment).anyTimes();

        replay(experimentDAO);

        atlasNetCDFDAO = new AtlasNetCDFDAO();
        atlasNetCDFDAO.setAtlasDataRepo(new File(getClass().getClassLoader().getResource("").getPath()));

        atlasNetCDFDAO.setExperimentDAO(experimentDAO);
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
        assertNotNull("Got null for design element ID", ea.getDesignElementID());
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


        assertEquals(designElementIdForMinPValue, Long.valueOf(ea.getDesignElementID()));
        assertEquals(pValFormat.format(minPValue), pValFormat.format(ea.getPValAdjusted()));
    }
}

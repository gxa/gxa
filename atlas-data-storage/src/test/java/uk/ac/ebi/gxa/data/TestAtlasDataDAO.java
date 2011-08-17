package uk.ac.ebi.gxa.data;

import com.google.common.base.Predicates;
import junit.framework.TestCase;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * This class tests functionality of AtlasDataDAO
 *
 * @author Robert Petryszak
 */
public class TestAtlasDataDAO extends TestCase {
    private AtlasDataDAO atlasDataDAO;
    private Long geneId;
    private Experiment experiment;
    private String ef;
    private String efv;
    private float minPValue;
    private String designElementAccessionForMinPValue;
    private Set<Long> geneIds;
    private ArrayDesign arrayDesign;
    private final static DecimalFormat pValFormat = new DecimalFormat("0.#######");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        geneId = 153070209l; // human brca1
        arrayDesign = new ArrayDesign("A-AFFY-33");
        ef = "cell_type";
        efv = "germ cell";
        minPValue = 0.9999986f;
        designElementAccessionForMinPValue = "204531_s_at";

        experiment = new Experiment(411512559L, "E-MTAB-25");
        final Assay assay = new Assay("AssayAccession");
        assay.setArrayDesign(arrayDesign);
        experiment.setAssays(Collections.singletonList(assay));

        atlasDataDAO = new AtlasDataDAO();
        atlasDataDAO.setAtlasDataRepo(new File(getClass().getClassLoader().getResource("").getPath()));

        geneIds = new HashSet<Long>();
        geneIds.add(geneId);
    }

    public void testGetFactorValues() throws IOException, AtlasDataException {
        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(experiment);
        try {
            final String[] fvs = ewd.getFactorValues(arrayDesign, ef);
            assertNotNull(fvs);
            assertNotSame(fvs.length, 0);
            assertTrue(Arrays.asList(fvs).contains(efv));
        } finally {
            ewd.closeAllDataSources();
        }
    }

    public void testGetExpressionAnalyticsByGeneID() throws AtlasDataException {
        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(experiment);
        try {
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                ewd.getExpressionAnalysesForGeneIds(geneIds, Predicates.<ArrayDesign>alwaysTrue());
        
            // check the returned data
            assertNotNull(geneIdsToEfToEfvToEA.get(geneId));
            assertNotNull(geneIdsToEfToEfvToEA.get(geneId).get(ef));
            ExpressionAnalysis ea = geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv);
        
            assertNotNull(ea);
            assertNotNull("Got null for design element ID", ea.getDesignElementAccession());
            //assertNotNull("Got null for experiment ID", ea.getExperimentID());
            assertNotNull("Got null for ef name", ea.getEfName());
            assertNotNull("Got null for efv name", ea.getEfvName());
            assertNotNull("Got null for ef id", ea.getEfId());
            assertNotNull("Got null for efv id", ea.getEfvId());
            assertNotNull("Got null for pvalue", ea.getPValAdjusted());
            assertNotNull("Got null for tstat", ea.getTStatistic());
            assertNotNull("Got null for arrayDesign accession", ea.getArrayDesignAccession());
            assertNotNull("Got null for design element index", ea.getDesignElementIndex());
            System.out.println("Got expression analysis for gene id: " + geneId + " \n" + ea.toString());
        
        
            assertEquals(designElementAccessionForMinPValue, ea.getDesignElementAccession());
            assertEquals(pValFormat.format(minPValue), pValFormat.format(ea.getPValAdjusted()));
        } finally {
            ewd.closeAllDataSources();
        }
    }
}

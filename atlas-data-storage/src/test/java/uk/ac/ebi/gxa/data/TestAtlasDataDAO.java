package uk.ac.ebi.gxa.data;

import junit.framework.TestCase;
import uk.ac.ebi.gxa.utils.ResourceUtil;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.data.ExperimentPartCriteria.experimentPart;

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
        atlasDataDAO.setAtlasDataRepo(ResourceUtil.getResourceRoot(getClass()));

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
            closeQuietly(ewd);
        }
    }

    public void testGetExpressionAnalyticsByGeneID() throws AtlasDataException, StatisticsNotFoundException {
        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(experiment);
        try {
            ExperimentPart expPart = experimentPart().containsGenes(geneIds).retrieveFrom(ewd);
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    expPart.getExpressionAnalysesForGeneIds(geneIds);

            // check the returned data
            assertNotNull(geneIdsToEfToEfvToEA.get(geneId));
            assertNotNull(geneIdsToEfToEfvToEA.get(geneId).get(ef));
            ExpressionAnalysis ea = geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv);

            assertNotNull(ea);
            assertNotNull("Got null for design element ID", ea.getDeAccession());
            //assertNotNull("Got null for experiment ID", ea.getExperimentID());
            assertNotNull("Got null for ef name", ea.getEfv().getFirst());
            assertNotNull("Got null for efv name", ea.getEfv().getSecond());
            assertNotNull("Got null for pvalue", ea.getP());
            assertNotNull("Got null for tstat", ea.getT());
            assertNotNull("Got null for arrayDesign accession", ea.getArrayDesignAccession());
            assertNotNull("Got null for design element index", ea.getDeIndex());
            System.out.println("Got expression analysis for gene id: " + geneId + " \n" + ea.toString());


            assertEquals(designElementAccessionForMinPValue, ea.getDeAccession());
            assertEquals(pValFormat.format(minPValue), pValFormat.format(ea.getP()));
        } finally {
            closeQuietly(ewd);
        }
    }
}

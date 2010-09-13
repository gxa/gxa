package uk.ac.ebi.gxa.netcdf.reader;

import junit.framework.TestCase;
import uk.ac.ebi.microarray.atlas.model.AtlasCount;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class tests functionality of AtlasNetCDFDAO
 *
 * @author Rober Petryszak
 * @date 13-Sep-2010
 */
public class TestNetCDFDAO extends TestCase {

    private File netCDFRepoLocation;
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private Long geneId;
    private String experimentId;
    private String ef = "cell_type";
    private String efv = "germ cell";


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        geneId = 153070209l; // human brca1
        experimentId = "411512559";  // E-MTAB-25

        netCDFRepoLocation = new File("target" + File.separator + "test-classes");
        atlasNetCDFDAO = new AtlasNetCDFDAO();
        atlasNetCDFDAO.setAtlasNetCDFRepo(netCDFRepoLocation);
    }

    public void testGetExpressionAnalyticsByGeneID() throws IOException {
        try {
            Set<Long> geneIds = new HashSet<Long>();
            geneIds.add(geneId);
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    atlasNetCDFDAO.getExpressionAnalysesForGeneIds(geneIds, experimentId);

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
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test the NetCDF equivalent of the following query based on data in a2_expressionanalytics. Essentially
     * for a given experimenId, the query finds the min pValue for each ef/efv combination and the counts the number
     * of genes that have that ef-efv-min pvalue combination:
     * <p/>
     * SELECT
     * ea.experimentid,
     * p.name AS property,
     * pv.name AS propertyvalue,
     * CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END AS updn,
     * min(ea.pvaladj),
     * COUNT(DISTINCT(g.geneid)) AS genes,
     * min(p.propertyid) AS propertyid,
     * min(pv.propertyvalueid)  AS propertyvalueid
     * <p/>
     * FROM a2_expressionanalytics ea
     * JOIN a2_propertyvalue pv ON pv.propertyvalueid=ea.propertyvalueid
     * JOIN a2_property p ON p.propertyid=pv.propertyid
     * JOIN a2_designelement de ON de.designelementid=ea.designelementid
     * JOIN a2_gene g ON g.geneid=de.geneid;
     * WHERE ea.experimentid=?
     * <p/>
     * GROUP BY ea.experimentid, p.name, pv.name,
     * CASE WHEN ea.tstat < 0 THEN -1 ELSE 1 END;
     */
    public void testGetAtlasCountsByExperimentID() {
        try {
            Set<Long> geneIds = atlasNetCDFDAO.getGeneIds(experimentId);
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    atlasNetCDFDAO.getExpressionAnalysesForGeneIds(geneIds, experimentId);

            Map<String, Map<String, AtlasCount>> efToEfvToAtlasCount = new HashMap<String, Map<String, AtlasCount>>();

            for (Long geneId : geneIdsToEfToEfvToEA.keySet()) {
                Map<String, Map<String, ExpressionAnalysis>> efToEfvToEA =
                        geneIdsToEfToEfvToEA.get(geneId);
                for (String ef : efToEfvToEA.keySet()) {
                    Map<String, ExpressionAnalysis> efvToEA = efToEfvToEA.get(ef);
                    for (String efv : efvToEA.keySet()) {
                        ExpressionAnalysis bestEA = efvToEA.get(efv);
                        if (bestEA != null) {
                            Map<String, AtlasCount> efvToAtlasCount = efToEfvToAtlasCount.get(ef);
                            if (efvToAtlasCount == null) {
                                efvToAtlasCount = new HashMap<String, AtlasCount>();
                            }
                            AtlasCount ac = efvToAtlasCount.get(efv);
                            if (ac == null) {
                                ac = new AtlasCount();
                            }
                            ac.setProperty(ef);
                            ac.setPropertyValue(efv);
                            ac.setGeneCount(ac.getGeneCount() + 1);
                            ac.setUpOrDown(bestEA.getTStatistic() < 0 ? "-1" : "+1");
                            efvToAtlasCount.put(efv, ac);
                            efToEfvToAtlasCount.put(ef, efvToAtlasCount);
                        }
                    }
                }
            }

            List<AtlasCount> atlasCounts = new ArrayList<AtlasCount>();
            for (String ef : efToEfvToAtlasCount.keySet()) {
                Map<String, AtlasCount> efvToAtlasCount = efToEfvToAtlasCount.get(ef);
                for (String efv : efvToAtlasCount.keySet()) {
                    atlasCounts.add(efvToAtlasCount.get(efv));
                }
            }

            // check the returned data
            assertNotSame("Zero atlas counts returned", atlasCounts.size(), 0);
            for (AtlasCount atlas : atlasCounts) {
                assertNotNull(atlas);
                assertNotNull("Got null property", atlas.getProperty());
                assertNotSame("Got null property value", atlas.getPropertyValue());
                assertNotNull("Got null updn" + atlas.getUpOrDown());
                assertNotNull("Got 0 gene count" + atlas.getGeneCount());
                System.out.println("AtlasCount: " + atlas.toString());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}

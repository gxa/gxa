package uk.ac.ebi.gxa.netcdf.reader;

import junit.framework.TestCase;
import uk.ac.ebi.microarray.atlas.model.AtlasCount;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
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
    private String ef;
    private String efv;
    private float minPValue;
    private Long designElementIdForMinPValue;
    private String efvInMoreThanOneProxy;
    private String efvInMoreThanOneProxy1;
    Set<Long> geneIds;
    private String proxyId;
    private final static DecimalFormat pValFormat = new DecimalFormat("0.#######");
    private Set<String> proxyIds = new HashSet<String>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        geneId = 153070209l; // human brca1
        experimentId = "411512559";  // E-MTAB-25
        proxyId = "411512559_153069949.nc";
        ef = "cell_type";
        efv = "germ cell";
        minPValue = 0.8996214f;
        efvInMoreThanOneProxy = "CD4+ T cell";
        efvInMoreThanOneProxy1 = "CD8+ T cell";
        designElementIdForMinPValue = 153085549l;

        netCDFRepoLocation = new File("target" + File.separator + "test-classes");
        atlasNetCDFDAO = new AtlasNetCDFDAO();
        atlasNetCDFDAO.setAtlasNetCDFRepo(netCDFRepoLocation);
        geneIds = new HashSet<Long>();
        geneIds.add(geneId);

        proxyIds.add(proxyId);
        proxyIds.add("411512559_221532256.nc");
        proxyIds.add("411512559_222525156.nc");

    }

    public void testGetGeneIds() throws IOException {
        Set<Long> allGeneIds = atlasNetCDFDAO.getGeneIds(experimentId);
        assertNotNull(allGeneIds);
        assertNotSame(allGeneIds.size(), 0);
        assertTrue(allGeneIds.contains(geneId));

    }

    public void testGetProxyIdToDesignElements() throws IOException {
        Map<String, List<Long>> proxyIdToDesignElements = atlasNetCDFDAO.getProxyIdToDesignElements(experimentId);
        for (String proxyIdentifier : proxyIdToDesignElements.keySet()) {
            List<Long> deIds = proxyIdToDesignElements.get(proxyIdentifier);
            assertNotNull(deIds);
            assertNotSame(deIds.size(), 0);
            if (proxyId.equals(proxyIdentifier)) {
                assertTrue(deIds.contains(designElementIdForMinPValue));
            }
        }
    }

    public void testGetUniqueFactorValues() throws IOException {
        List<String> uniqueFVs = atlasNetCDFDAO.getUniqueFactorValues(experimentId, ef);
        assertNotNull(uniqueFVs);
        assertNotSame(uniqueFVs.size(), 0);
        assertTrue(uniqueFVs.contains(efv));
    }

    public void testGetFactorValues() throws IOException {
        List<String> fvs = atlasNetCDFDAO.getFactorValues(proxyId, ef);
        assertNotNull(fvs);
        assertNotSame(fvs.size(), 0);
        assertTrue(fvs.contains(efv));
    }

    public void testGetFactorValuesForExperiment() throws IOException {
        Map<String, List<String>> proxyIdToFvs = atlasNetCDFDAO.getFactorValuesForExperiment(experimentId, ef);
        List<String> fvs = proxyIdToFvs.get(proxyId);
        assertNotNull(fvs);
        assertNotSame(fvs.size(), 0);
        assertTrue(fvs.contains(efv));

        HashSet efvsAcrossMoreThanOneProxy = null;
        for (String proxyId : proxyIdToFvs.keySet()) {
            List<String> efvs = proxyIdToFvs.get(proxyId);
            if (efvsAcrossMoreThanOneProxy == null) {
                efvsAcrossMoreThanOneProxy = new HashSet(efvs);
            } else {
                efvsAcrossMoreThanOneProxy.retainAll(efvs);
            }
        }
        assertNotSame(efvsAcrossMoreThanOneProxy.size(), 2);
        assert (efvsAcrossMoreThanOneProxy.contains(efvInMoreThanOneProxy));
        assert (efvsAcrossMoreThanOneProxy.contains(efvInMoreThanOneProxy1));
    }

    public void testGetProxyIdToArrayDesignId() throws IOException {
        Map<String, Long> proxyIdToArrayDesignIds = atlasNetCDFDAO.getProxyIdToArrayDesignId(experimentId);
        for (String proxyIdentifier : proxyIdToArrayDesignIds.keySet()) {
            Long arrayDesignId = proxyIdToArrayDesignIds.get(proxyIdentifier);
            assertNotNull(arrayDesignId);
            assertNotSame(arrayDesignId, 0);
        }
    }


    public void testGetExpressionAnalyticsByGeneID() throws IOException {
        try {

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


            assertEquals(Long.valueOf(ea.getDesignElementID()), designElementIdForMinPValue);
            assertEquals(pValFormat.format(ea.getPValAdjusted()), pValFormat.format(minPValue));

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
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}

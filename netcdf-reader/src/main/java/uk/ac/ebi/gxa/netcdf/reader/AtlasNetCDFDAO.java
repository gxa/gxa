package uk.ac.ebi.gxa.netcdf.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

/**
 * This class wraps the functionality of retrieving values across multiple instances of NetCDFProxy
 *
 * @author Rober Petryszak
 * @date 13-Sep-2010
 */
public class AtlasNetCDFDAO {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Location of the NetCDF proxy files
    private File atlasNetCDFRepo;

    /**
     * @param atlasNetCDFRepo
     */
    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }

    public String getAtlasNetCDFRepoPath() {
        return atlasNetCDFRepo.getAbsolutePath();
    }

    /**
     *
     * @param experimentID
     * @return Set of unique gene ids across all proxies corresponding to experimentID
     * @throws IOException
     */
    public Set<Long> getGeneIds(final String experimentID) throws IOException {
        Set<Long> geneIds = new LinkedHashSet<Long>();
        List<NetCDFProxy> proxies = getNetCDFProxiesForExperiment(experimentID);
        try {
            for (NetCDFProxy proxy : proxies) {
                geneIds.addAll(getGeneIds(proxy));
            }
        } finally {
            close(proxies);
        }
        return geneIds;
    }


    /**
     * @param experimentID
     * @return Map: proxyId (i.e .proxy file name) -> List of design element indexes; for all proxies corresponding to experimentID
     * @throws IOException
     */
    public Map<String, List<Long>> getProxyIdToDesignElements(final String experimentID) throws IOException {
        // Get NetCDF proxies for this experimentId
        Map<String, List<Long>> proxyIdToDesignElements = new HashMap<String, List<Long>>();
        List<NetCDFProxy> proxies = getNetCDFProxiesForExperiment(experimentID);
        try {
            for (NetCDFProxy proxy : proxies) {
                proxyIdToDesignElements.put(proxy.getId(), getDesignElementIds(proxy));
            }
        } finally {
            close(proxies);
        }

        return proxyIdToDesignElements;
    }


    /**
     * @param experimentID
     * @param factor
     * @return List of unique factor values for a given experimental factor, across all proxies corresponding to experimentID
     * @throws IOException
     */
    public List<String> getUniqueFactorValues(final String experimentID, final String factor) throws IOException {
        // Get NetCDF proxies for this experimentId
        Set<String> factorsValsAcrossAllProxies = new LinkedHashSet<String>();
        List<NetCDFProxy> proxies = getNetCDFProxiesForExperiment(experimentID);
        try {
            for (NetCDFProxy proxy : proxies) {
                factorsValsAcrossAllProxies.addAll(Arrays.asList(proxy.getFactorValues(factor)));
            }
        } finally {
            close(proxies);
        }
        return new ArrayList<String>(factorsValsAcrossAllProxies);
    }


    /**
     * @param experimentID
     * @return Map proxyId -> array design id, across all proxies corresponding to experimentID
     * @throws IOException
     */
    public Map<String, Long> getProxyIdToArrayDesignId(final String experimentID) throws IOException {
        // Get NetCDF proxies for this experimentId
        Map<String, Long> proxyIdToArrayDesignIds = new HashMap<String, Long>();
        List<NetCDFProxy> proxies = getNetCDFProxiesForExperiment(experimentID);
        try {
            for (NetCDFProxy proxy : proxies) {
                proxyIdToArrayDesignIds.put(proxy.getId(), proxy.getArrayDesignID());
            }
        } finally {
            close(proxies);
        }
        return proxyIdToArrayDesignIds;
    }


    /**
     * @param geneIds
     * @param experimentID
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws IOException
     */

    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(
            final Set<Long> geneIds,
            final String experimentID
    ) throws IOException {

        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                new HashMap<Long, Map<String, Map<String, ExpressionAnalysis>>>();


        // Get NetCDF proxies for this experimentId
        List<NetCDFProxy> proxies = getNetCDFProxiesForExperiment(experimentID);
        try {
            for (NetCDFProxy proxy : proxies) {
                // Map gene ids to design element ids in which those genes are present
                Map<Long, List<Integer>> geneIdToDEIndexes =
                        getGeneIdToDesignElementIndexes(proxy, geneIds);
                proxy.addExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes, geneIdsToEfToEfvToEA);
            }
        } finally {
            close(proxies);
        }
        return geneIdsToEfToEfvToEA;
    }

    /**
     * Retrieve a list of numOfTopGenes Pairs: geneId -> ExpressionAnalysis corresponding to a min pVals
     * across all ef-efvs in experimentID; if geneIds is non empty, find the top genes from among that set
     * only. Otherwise, top genes should be foudn from among all of the genes with expression data in experimentID.
     * @param experimentID experiment in which top genes should be found
     * @param geneIds a list of genes from among which the list of top genes should be found. Note that if
     * some/all of genes in geneIds have no expression data in experimentID, the returned
     * list will not contain those missing genes.
     * @param rows the maximum number of top genes to be found
     * @return a list of numOfTopGenes Pairs: geneId -> ExpressionAnalysis corresponding to a min pVal
     * @throws IOException
     */
    public List<Pair<Long, ExpressionAnalysis>> getTopNGeneIdsToMinPValForExperiment(
            final String experimentID,
            Set<Long> geneIds,
            final Integer rows) throws IOException {
        List<Pair<Long, ExpressionAnalysis>> results = new ArrayList<Pair<Long, ExpressionAnalysis>>();

        Set<Long> returnedGenes = new HashSet<Long>();

        // TreeMap is used so that its keySet() of pVals is sorted in ascending order
        Map<Float, List<Pair<Long, ExpressionAnalysis>>> auxPValToGeneId =
                new TreeMap<Float, List<Pair<Long, ExpressionAnalysis>>>();

        if (geneIds.isEmpty()) {
            geneIds = getGeneIds(experimentID);
        }
        // Retrieve geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination in experimentId
        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                getExpressionAnalysesForGeneIds(geneIds, experimentID);

        // Iterate over geneIdsToEfToEfvToEA, to retrieve sorted map of pValues, auxPValToGeneId
        for (Long geneId : geneIdsToEfToEfvToEA.keySet()) {
            Map<String, Map<String, ExpressionAnalysis>> efToEfvToEA = geneIdsToEfToEfvToEA.get(geneId);
            for (String ef : efToEfvToEA.keySet()) {
                Map<String, ExpressionAnalysis> efvToEA = efToEfvToEA.get(ef);
                for (String efv : efvToEA.keySet()) {
                    Pair<Long, ExpressionAnalysis> geneIdToEA = new Pair<Long, ExpressionAnalysis>(geneId, efvToEA.get(efv));
                    List<Pair<Long, ExpressionAnalysis>> geneToEAsForPVal = auxPValToGeneId.get(efvToEA.get(efv).getPValAdjusted());
                    if (geneToEAsForPVal == null) {
                        geneToEAsForPVal = new ArrayList<Pair<Long, ExpressionAnalysis>>();
                    }
                    geneToEAsForPVal.add(geneIdToEA);
                    auxPValToGeneId.put(efvToEA.get(efv).getPValAdjusted(), geneToEAsForPVal);
                }
            }
        }
        for (Float pValue : auxPValToGeneId.keySet()) {
            List<Pair<Long, ExpressionAnalysis>> geneToEAsForPVal = auxPValToGeneId.get(pValue);

            // Sort geneToEAsForPVal in desc order by Math.abst(stat) -- for the same pValue in both ExpressionAnalyses,
            // he better one is the one with the higher absolute value of tstat
            Collections.sort(geneToEAsForPVal, new Comparator<Pair<Long, ExpressionAnalysis>>() {
                public int compare(Pair<Long, ExpressionAnalysis> p1, Pair<Long, ExpressionAnalysis> p2) {
                    return Float.valueOf(Math.abs(p2.getSecond().getTStatistic())).compareTo(Float.valueOf(Math.abs(p1.getSecond().getTStatistic())));
                }
            });

            for (Pair<Long, ExpressionAnalysis> geneIdToEA : geneToEAsForPVal) {
                if (results.size() == rows) {
                    break;
                }
                Long geneId = geneIdToEA.getFirst();
                if (!returnedGenes.contains(geneId)) {
                    // Genes may have multiple entries in auxPValToGeneId - for differrent ef-efv combinations - we need to
                    // return only one entry per gene - the one with the best value across all ef-efv combinations, i.e  the one
                    // at the earliest position of the TreeMap auxPValToGeneId's pValue keySet() (itself sorted in asc order)
                    returnedGenes.add(geneId);
                    results.add(geneIdToEA);
                }
            }
        }

        return results;
    }

    

    /**
     *
     * @param proxyId
     * @param factor
     * @return factor values for factor in proxyId
     * @throws IOException
     */
    public List<String> getFactorValues(final String proxyId, final String factor) throws IOException {
        NetCDFProxy proxy = getNetCDFProxy(proxyId);
        String[] factorValues;
        try {
            factorValues = proxy.getFactorValues(factor);
            return Arrays.asList(factorValues);
        } finally {
            close(proxy);
        }
    }

    /**
     *
     * @param experimentID
     * @param factor
     * @return Map proxyId -> factor values, across all proxies corresponding to experimentID
     * @throws IOException
     */
    public Map<String, List<String>> getFactorValuesForExperiment
            (final String experimentID, final String factor) throws IOException {
        Map<String, List<String>> proxyIdToFvs = new HashMap<String, List<String>>();
        // Get NetCDF proxies for this experimentId
        List<NetCDFProxy> proxies = getNetCDFProxiesForExperiment(experimentID);
        try {
            for (NetCDFProxy proxy : proxies) {
                proxyIdToFvs.put(proxy.getId(), new ArrayList<String>(Arrays.asList(proxy.getFactorValues(factor))));
            }
        } finally {
            close(proxies);
        }
        return proxyIdToFvs;
    }

    /**
     *
     * @param proxyId
     * @param designElementIndex
     * @return List of expression values retrieved from designElementIndex in proxyId
     * @throws IOException
     */
    public List<Float> getExpressionData(final String proxyId, final Integer designElementIndex)
            throws IOException {
        NetCDFProxy proxy = getNetCDFProxy(proxyId);
        float[] expressionDataArr = null;
        try {
            expressionDataArr = proxy.getExpressionDataForDesignElementAtIndex(designElementIndex);
        } finally {
            close(proxy);
        }
        List<Float> expressionData = new ArrayList<Float>();
        for (int i = 0; i < expressionDataArr.length; i++) {
            expressionData.add(expressionDataArr[i]);
        }

        return expressionData;
    }

    /**
     *
     * @param proxyId
     * @return Array design accession retrieved from proxyId
     * @throws IOException
     */
    public String getArrayDesignAccession(final String proxyId) throws IOException {
        NetCDFProxy proxy = getNetCDFProxy(proxyId);
        String arrayDesignAcc = null;
        try {
            arrayDesignAcc = proxy.getArrayDesignAccession();
        } finally {
            close(proxy);
        }
        return arrayDesignAcc;
    }

       /**
     * @param proxyId
     * @return NetCDFProxy for a given proxyId (i.e. proxy file name)
     */
    public NetCDFProxy getNetCDFProxy(String proxyId) {
        assert (atlasNetCDFRepo != null);
        return new NetCDFProxy(new File(atlasNetCDFRepo + File.separator + proxyId));
    }

    /**
     * Close all NetCDF proxies ih the argument list
     *
     * @param proxies
     */

    private void close(List<NetCDFProxy> proxies) {
        for (NetCDFProxy proxy : proxies) {
            close(proxy);
        }
    }

    /**
     * Close proxy
     *
     * @param proxy
     */
    public void close(NetCDFProxy proxy) {
        try {
            proxy.close();
        } catch (IOException ioe) {
            log.error("Failed to close NetCDF proxy: " + proxy.getId(), ioe);
        }
    }

       /**
     * @param experimentID
     * @return List of NetCDF proxies corresponding to experimentID
     */
    private List<NetCDFProxy> getNetCDFProxiesForExperiment(final String experimentID) {
        List<NetCDFProxy> proxies = new ArrayList<NetCDFProxy>();
        // lookup NetCDFFiles for this experiment
        File[] netCDFs = atlasNetCDFRepo.listFiles(new FilenameFilter() {

            public boolean accept(File file, String name) {
                return name.matches("^" + experimentID + "_[0-9]+(_ratios)?\\.nc$");
            }
        });

        for (File netCDF : netCDFs) {
            proxies.add(getNetCDFProxy(netCDF.getName()));
        }

        return proxies;
    }

    /**
     * @param proxy
     * @return List of geneIds in proxy
     * @throws IOException
     */
    private List<Long> getGeneIds(final NetCDFProxy proxy) throws IOException {
        List<Long> geneIds = new ArrayList<Long>();
        long[] geneIdsForProxy = proxy.getGenes();
        for (int i = 0; i < geneIdsForProxy.length; i++) {
            geneIds.add(geneIdsForProxy[i]);
        }
        return geneIds;
    }

        /**
     * @param proxy
     * @return List of design element ids in proxy
     * @throws IOException
     */
    private List<Long> getDesignElementIds(final NetCDFProxy proxy) throws IOException {
        List<Long> deIds = new ArrayList<Long>();
        long[] geneIdsForProxy = proxy.getDesignElements();
        for (int i = 0; i < geneIdsForProxy.length; i++) {
            deIds.add(geneIdsForProxy[i]);
        }
        return deIds;
    }

      /**
     * @param proxy
     * @param geneIds
     * @return Map: geneId -> List of design element indexes in proxy
     * @throws IOException
     */
    private Map<Long, List<Integer>> getGeneIdToDesignElementIndexes(
            final NetCDFProxy proxy,
            final Set<Long> geneIds)
            throws IOException {
        // Note that in a given NetCDF proxy more than one geneIndex (==designElementIndex) may correspond to one geneId
        // (i.e. proxy.getGenes() may contain duplicates, whilst proxy.getDesignElements() will not; and
        // proxy.getGenes().size() == proxy.getDesignElements().size())
        Map<Long, List<Integer>> geneIdToDEIndexes = new HashMap<Long, List<Integer>>();

        // Get gene ids present in proxy
        List<Long> geneIdsInProxy = getGeneIds(proxy);

        int deIndex = 0;
        for (Long geneId : geneIdsInProxy) {
            if (geneIds.contains(geneId)) {
                List<Integer> deIndexes = geneIdToDEIndexes.get(geneId);
                if (deIndexes == null) {
                    deIndexes = new ArrayList<Integer>();
                }
                deIndexes.add(deIndex);
                geneIdToDEIndexes.put(geneId, deIndexes);
            }
            deIndex++;
        }
        return geneIdToDEIndexes;
    }

    /**
     * @param proxyId
     * @param geneId
     * @param ef
     * @return Map: efv -> best ExpressionAnalysis for geneid-ef in this proxy
     * @throws IOException
     */
    public Map<String, ExpressionAnalysis> getBestEAsPerEfvInProxy(
            final String proxyId,
            final Long geneId,
            final String ef)
            throws IOException {
        Set<Long> geneIds = new HashSet<Long>();
        geneIds.add(geneId);

        NetCDFProxy proxy = getNetCDFProxy(proxyId);
        try {
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    new HashMap<Long, Map<String, Map<String, ExpressionAnalysis>>>();
            Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(proxy, geneIds);
            proxy.addExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes, geneIdsToEfToEfvToEA);
            return geneIdsToEfToEfvToEA.get(geneId).get(ef);
        } finally {
            close(proxy);
        }
    }
}

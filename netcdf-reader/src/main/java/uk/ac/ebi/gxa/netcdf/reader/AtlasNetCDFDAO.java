package uk.ac.ebi.gxa.netcdf.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.*;
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
     * @param geneIds
     * @param experimentID
     * @param proxy
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws IOException
     */

    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(
            final Set<Long> geneIds,
            final String experimentID,
            NetCDFProxy proxy
    ) throws IOException {

        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                new HashMap<Long, Map<String, Map<String, ExpressionAnalysis>>>();
        // Find first proxy for experimentID if proxy was not passed in
        if (proxy == null) {
            proxy = new NetCDFProxy(new File(findProxyId(experimentID, null)));
        }
        try {
            // Map gene ids to design element ids in which those genes are present
            Map<Long, List<Integer>> geneIdToDEIndexes =
                    getGeneIdToDesignElementIndexes(proxy, geneIds);
            geneIdsToEfToEfvToEA = proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes);
        } finally {
            if (proxy != null) {
                proxy.close();
            }
        }
        return geneIdsToEfToEfvToEA;
    }

    /**
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
            proxy.close();
        }
        List<Float> expressionData = new ArrayList<Float>();
        for (int i = 0; i < expressionDataArr.length; i++) {
            expressionData.add(expressionDataArr[i]);
        }

        return expressionData;
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
     * @param experimentID
     * @param arrayDesignAcc Array Design accession
     * @return if arrayDesignAcc != null, id of first proxy for experimentID, that matches arrayDesignAcc;
     *         otherwise, id of first proxy in the list returned by getNetCDFProxiesForExperiment()
     */
    public String findProxyId(final String experimentID, final String arrayDesignAcc) {
        List<NetCDFProxy> proxies = getNetCDFProxiesForExperiment(experimentID);
        String proxyId = null;
        for (NetCDFProxy proxy : proxies) {
            String adAcc = null;
            try {
                adAcc = proxy.getArrayDesignAccession();
            } catch (IOException ioe) {
                log.error("Failed to retrieve array design accession for a proxy for experiment id: " + experimentID);
            }
            if (proxyId == null && (arrayDesignAcc == null || arrayDesignAcc.equals(adAcc))) {
                proxyId = proxy.getId();
            }
            proxy.close();
        }
        return proxyId;
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
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA;
            Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(proxy, geneIds);
            geneIdsToEfToEfvToEA = proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes);
            return geneIdsToEfToEfvToEA.get(geneId).get(ef);
        } finally {
            if (proxy != null) {
                proxy.close();
            }
        }
    }

    /**
     *
     * @return List of all NetCDF Files in atlasNetCDFRepo
     */
    public List<File> getAllNcdfs() {
                return Arrays.asList(atlasNetCDFRepo.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".nc"));
            }
        }));
    }
}

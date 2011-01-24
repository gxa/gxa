package uk.ac.ebi.gxa.netcdf.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * This class wraps the functionality of retrieving values across multiple instances of NetCDFProxy
 *
 * @author Rober Petryszak
 */
public class AtlasNetCDFDAO {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Location of the experiment data files
    private File atlasDataRepo;

    public void setAtlasDataRepo(File atlasDataRepo) {
        this.atlasDataRepo = atlasDataRepo;
    }

    /**
     * @param geneIds
     * @param experimentAccession
     * @param proxy
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws IOException
     */

    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(
            final Set<Long> geneIds,
            final String experimentAccession,
            NetCDFProxy proxy) throws IOException {
        try {
            // Find first proxy for experimentAccession if proxy was not passed in
            if (proxy == null) {
                proxy = new NetCDFProxy(new File(atlasDataRepo, findProxyId(experimentAccession, null, geneIds)));
            }
            // Map gene ids to design element ids in which those genes are present
            Map<Long, List<Integer>> geneIdToDEIndexes =
                    getGeneIdToDesignElementIndexes(proxy, geneIds);
            return proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes);
        } finally {
            closeQuietly(proxy);
        }
    }

    /**
     * @param proxyId
     * @param designElementIndex
     * @return List of expression values retrieved from designElementIndex in proxyId
     * @throws IOException
     */
    public List<Float> getExpressionData(final String experimentAccession, final String proxyId, final Integer designElementIndex)
            throws IOException {
        NetCDFProxy proxy = null;
        float[] expressionDataArr = null;
        try {
            proxy = getNetCDFProxy(experimentAccession, proxyId);
            expressionDataArr = proxy.getExpressionDataForDesignElementAtIndex(designElementIndex);
        } finally {
            closeQuietly(proxy);
        }
        List<Float> expressionData = new ArrayList<Float>();
        for (float anExpressionDataArr : expressionDataArr) {
            expressionData.add(anExpressionDataArr);
        }
        return expressionData;
    }

    /**
     * @param proxyId
     * @return NetCDFProxy for a given proxyId (i.e. proxy file name)
     */
    public NetCDFProxy getNetCDFProxy(String experimentAccession, String proxyId) {
        assert (atlasDataRepo != null);
        return new NetCDFProxy(new File(getDataDirectory(experimentAccession), proxyId));
    }


    /**
     * @param experimentAccession
     * @param arrayDesignAcc      Array Design accession
     * @param geneIds             data for these gene ids is required to exist in the found proxy
     * @return if arrayDesignAcc != null, id of first proxy for experimentAccession, that matches arrayDesignAcc;
     *         otherwise, id of first proxy in the list returned by getNetCDFsForExperiment()
     */
    public String findProxyId(final String experimentAccession, final String arrayDesignAcc, final Set<Long> geneIds) {
        List<File> netcdfs = getNetCDFsForExperiment(experimentAccession);
        for (File file : netcdfs) {
            NetCDFProxy proxy = null;
            try {
                proxy = new NetCDFProxy(file);
                List<Long> geneIdsInProxy = null;
                String adAcc = null;

                try {
                    adAcc = proxy.getArrayDesignAccession();
                    geneIdsInProxy = getGeneIds(proxy);
                } catch (IOException ioe) {
                    if (adAcc == null) {
                        log.error("Failed to retrieve array design accession for a proxy for experiment accession: " + experimentAccession);
                    } else {
                        log.error("Failed to retrieve geneIds from a proxy for experiment accession: " + experimentAccession);
                    }
                }

                // if arrayDesignAcc was specified, it must match current proxy's array design (adAcc)
                if ((arrayDesignAcc != null && arrayDesignAcc.equals(adAcc)) ||
                        // if arrayDesignAcc was not specified then all geneIds must be found in this proxy
                        (arrayDesignAcc == null &&
                                (geneIds == null || geneIdsInProxy == null || geneIdsInProxy.containsAll(geneIds)))) {
                    return proxy.getId();
                }
            } finally {
                closeQuietly(proxy);
            }
        }
        return null;
    }

    /**
     * @param experimentAccession
     * @param arrayDesignAcc      Array Design accession
     * @param geneIds             data for these gene ids is required to exist in the found proxy
     */
    public File findNetCDF(final String experimentAccession, final String arrayDesignAcc, final Set<Long> geneIds) {
        List<File> netCDFs = getNetCDFsForExperiment(experimentAccession);
        for (File file : netCDFs) {
            NetCDFProxy proxy = null;
            try {
                proxy = new NetCDFProxy(file);
                List<Long> geneIdsInProxy = null;
                String adAcc = null;

                try {
                    adAcc = proxy.getArrayDesignAccession();
                    geneIdsInProxy = getGeneIds(proxy);
                } catch (IOException ioe) {
                    if (adAcc == null) {
                        log.error("Failed to retrieve array design accession for a proxy for experiment accession: " + experimentAccession);
                    } else {
                        log.error("Failed to retrieve geneIds from a proxy for experiment accession: " + experimentAccession);
                    }
                }

                // if arrayDesignAcc was specified, it must match current proxy's array design (adAcc)
                if ((arrayDesignAcc != null && arrayDesignAcc.equals(adAcc)) ||
                        // if arrayDesignAcc was not specified then all geneIds must be found in this proxy
                        (arrayDesignAcc == null &&
                                (geneIds == null || geneIdsInProxy == null || geneIdsInProxy.containsAll(geneIds)))) {
                    return file;
                }
            } finally {
                closeQuietly(proxy);
            }
        }
        return null;
    }

    public File[] listNetCDFs(String experimentAccession) {
        final String pattern = "^.+\\.nc$";
        File[] list = getDataDirectory(experimentAccession).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.matches(pattern);
            }
        });
        return list == null ? new File[0] : list;
    }

    public File getDataDirectory(String experimentAccession) {
        final String[] parts = experimentAccession.split("-");
        if (parts.length != 3 || !"E".equals(parts[0])) {
            throw new RuntimeException("Invalid experiment accession: " + experimentAccession);
        }
        final String num = (parts[2].length() > 2) ?
                parts[2].substring(0, parts[2].length() - 2) + "00" : "00";
        return new File(new File(new File(atlasDataRepo, parts[1]), num), experimentAccession);
    }

    /**
     * @param experimentAccession
     * @return List of NetCDF proxies corresponding to experimentAccession
     */
    private List<File> getNetCDFsForExperiment(String experimentAccession) {
        // lookup NetCDFFiles for this experiment
        return Arrays.asList(listNetCDFs(experimentAccession));
    }

    /**
     * @param proxy
     * @return List of geneIds in proxy
     * @throws IOException
     */
    private List<Long> getGeneIds(final NetCDFProxy proxy) throws IOException {
        List<Long> geneIds = new ArrayList<Long>();
        long[] geneIdsForProxy = proxy.getGenes();
        for (long aGeneIdsForProxy : geneIdsForProxy) {
            geneIds.add(aGeneIdsForProxy);
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

        int deIndex = 0;
        for (Long geneId : getGeneIds(proxy)) {
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
            final String experimentAccession,
            final String proxyId,
            final Long geneId,
            final String ef)
            throws IOException {
        Set<Long> geneIds = new HashSet<Long>();
        geneIds.add(geneId);

        NetCDFProxy proxy = null;
        try {
            proxy = getNetCDFProxy(experimentAccession, proxyId);
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA;
            Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(proxy, geneIds);
            geneIdsToEfToEfvToEA = proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes);
            return geneIdsToEfToEfvToEA.get(geneId).get(ef);
        } finally {
            closeQuietly(proxy);
        }
    }
}

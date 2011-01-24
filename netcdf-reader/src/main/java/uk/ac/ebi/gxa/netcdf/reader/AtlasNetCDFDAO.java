/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.netcdf.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.FileUtil;
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

    public void removeExperimentData(String accession) {
        FileUtil.deleteDirectory(getDataDirectory(accession));
    }

    public void setAtlasDataRepo(File atlasDataRepo) {
        this.atlasDataRepo = atlasDataRepo;
    }

    /**
     * @param geneIds
     * @param experimentAccession
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws IOException
     */

    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(
            final Set<Long> geneIds,
            final String experimentAccession) throws IOException {
        NetCDFProxy proxy = null;
        try {
            // Find first proxy for experimentAccession if proxy was not passed in
            proxy = new NetCDFProxy(new File(getDataDirectory(experimentAccession), findProxyId(experimentAccession, null, geneIds)));
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
        NetCDFProxy proxy = getNetCDFProxy(experimentAccession, proxyId);
        float[] expressionDataArr = null;
        try {
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
    public NetCDFProxy getNetCDFProxy(String experimentAccession, String proxyId) throws IOException {
        return new NetCDFProxy(new File(getDataDirectory(experimentAccession), proxyId));
    }


    /**
     * @param experimentAccession
     * @param arrayDesignAcc      Array Design accession
     * @param geneIds             data for these gene ids is required to exist in the found proxy
     * @return if arrayDesignAcc != null, id of first proxy for experimentAccession, that matches arrayDesignAcc;
     *         otherwise, id of first proxy in the list returned by getNetCDFProxiesForExperiment()
     */
    private String findProxyId(final String experimentAccession, final String arrayDesignAcc, final Set<Long> geneIds) throws IOException {
        List<NetCDFProxy> proxies = getNetCDFProxiesForExperiment(experimentAccession);
        String proxyId = null;
        for (NetCDFProxy proxy : proxies) {
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

            if (proxyId == null) {
                // if arrayDesignAcc was specified, it must match current proxy's array design (adAcc)
                if ((arrayDesignAcc != null && arrayDesignAcc.equals(adAcc)) ||
                        // if arrayDesignAcc was not specified then all geneIds must be found in this proxy 
                        (arrayDesignAcc == null &&
                                (geneIds == null || geneIdsInProxy == null || geneIdsInProxy.containsAll(geneIds)))) {
                    proxyId = proxy.getId();
                }
            }
            closeQuietly(proxy);
        }
        return proxyId;
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
    private List<NetCDFProxy> getNetCDFProxiesForExperiment(String experimentAccession) throws IOException {
        // lookup NetCDFFiles for this experiment
        File[] netCDFs = listNetCDFs(experimentAccession);
        List<NetCDFProxy> proxies = new ArrayList<NetCDFProxy>(netCDFs.length);

        for (File netCDF : netCDFs) {
            proxies.add(getNetCDFProxy(experimentAccession, netCDF.getName()));
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
     * @param experimentAccession
     * @param geneId
     * @param ef
     * @param efv
     * @param isUp
     * @return best (UP if isUp == true; DOWN otherwise) ExpressionAnalysis for geneId-ef-efv in experimentAccession's
     *         first proxy in which expression data for that combination exists
     */
    public ExpressionAnalysis getBestEAForGeneEfEfvInExperiment(final String experimentAccession,
                                                                final Long geneId,
                                                                final String ef,
                                                                final String efv,
                                                                final boolean isUp) {
        ExpressionAnalysis ea = null;
        try {
            List<NetCDFProxy> proxies = getNetCDFProxiesForExperiment(experimentAccession);
            for (NetCDFProxy proxy : proxies) {
                if (ea == null) {
                    Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(proxy, Collections.singleton(geneId));
                    Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                            proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes, ef, efv, isUp);
                    if (geneIdsToEfToEfvToEA.containsKey(geneId) &&
                            geneIdsToEfToEfvToEA.get(geneId).containsKey(ef) &&
                            geneIdsToEfToEfvToEA.get(geneId).get(ef).containsKey(efv) &&

                            geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv) != null) {
                        ea = geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv);
                    }

                }
                closeQuietly(proxy);
            }
        } catch (IOException ioe) {
            log.error("Failed to ExpressionAnalysis for gene id: " + geneId + "; ef: " + ef + " ; efv: " + efv + " in experiment: " + experimentAccession);
        }
        return ea;
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

        NetCDFProxy proxy = getNetCDFProxy(experimentAccession, proxyId);
        try {
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA;
            Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(proxy, Collections.singleton(geneId));
            geneIdsToEfToEfvToEA = proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes);
            return geneIdsToEfToEfvToEA.get(geneId).get(ef);
        } finally {
            closeQuietly(proxy);
        }
    }

    /**
     * @return List of all NetCDF Files in atlasNetCDFRepo
     */
    public List<File> getAllNcdfs() {
        return getAllNcdfs(atlasDataRepo);
    }

    /**
     * @return List of all NetCDF Files in ncdfsDir
     */
    private List<File> getAllNcdfs(File ncdfsDir) {

        List<File> ncdfs = new ArrayList<File>();

        ncdfs.addAll(Arrays.asList(ncdfsDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".nc"));
            }
        })));

        if (ncdfs.isEmpty()) {
            List<File> files = Arrays.asList(ncdfsDir.listFiles());
            for (File file : files) {
                if (file.isDirectory())
                    ncdfs.addAll(getAllNcdfs(file));
            }
        }
        return ncdfs;
    }

    public File getNetCdfFile(String experimentAccession, String arrayDesignAccession, Set<Long> geneIds) {
        try {
            String proxyId = findProxyId(experimentAccession, arrayDesignAccession, geneIds);
            return proxyId == null ? null : new File(getDataDirectory(experimentAccession), proxyId);
        } catch (IOException e) {
            return null;
        }
    }

    List<String> getFactorValues(String experimentAccession, String proxyId, String ef) throws IOException {
        NetCDFProxy proxy = null;
        try {
            proxy = getNetCDFProxy(experimentAccession, proxyId);
            return Arrays.asList(proxy.getFactorValues(ef));
        } finally {
            closeQuietly(proxy);
        }
    }

    public List<String> getAssayFvs(String experimentalFactor, String experimentAccession, String proxyId) throws IOException {
        NetCDFProxy proxy = null;
        try {
            proxy = getNetCDFProxy(experimentAccession, proxyId);
            return Arrays.asList(proxy.getFactorValues(experimentalFactor));
        } finally {
            closeQuietly(proxy);
        }
    }
}

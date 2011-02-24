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
import uk.ac.ebi.gxa.utils.ZipUtil;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.io.Closeables.closeQuietly;
import static com.google.common.primitives.Floats.asList;
import static com.google.common.primitives.Longs.asList;
import static java.util.Collections.singleton;
import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;
import static uk.ac.ebi.gxa.utils.FileUtil.extension;

/**
 * This class wraps the functionality of retrieving values across multiple instances of NetCDFProxy
 *
 * @author Rober Petryszak
 */
public class AtlasNetCDFDAO {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Location of the experiment data files
    private File atlasDataRepo;

    private static String getFilename(Experiment experiment, ArrayDesign arrayDesign) {
        return experiment.getExperimentID() + "_" + arrayDesign.getArrayDesignID() + ".nc";
    }

    public File getNetCDFLocation(Experiment experiment, ArrayDesign arrayDesign) {
        return new File(getDataDirectory(experiment.getAccession()), getFilename(experiment, arrayDesign));
    }

    public void removeExperimentData(String accession) {
        FileUtil.deleteDirectory(getDataDirectory(accession));
    }

    public void setAtlasDataRepo(File atlasDataRepo) {
        this.atlasDataRepo = atlasDataRepo;
    }

    public void releaseExperiment(String accession) throws IOException {
        File directory = getDataDirectory(accession);

        File exportFolder = new File(atlasDataRepo, "export");
        if (!exportFolder.exists() && !exportFolder.mkdirs()) {
            throw new FileNotFoundException("can not create export folder " + exportFolder);
        }

        ZipUtil.compress(directory, new File(exportFolder, accession + ".zip"));
    }

    /**
     * @param geneIds
     * @param experimentAccession
     * @param experimentId
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws IOException
     */
    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(
            final Set<Long> geneIds,
            final String experimentAccession,
            final long experimentId) throws IOException {
        NetCDFProxy proxy = null;
        try {
            // Find first proxy for experimentAccession if proxy was not passed in
            proxy = findNetcdf(experimentAccession, experimentId, null, geneIds).createProxy();
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
        try {
            proxy = getNetCDFProxy(experimentAccession, proxyId);
            return asList(proxy.getExpressionDataForDesignElementAtIndex(designElementIndex));
        } finally {
            closeQuietly(proxy);
        }
    }

    /**
     * @param proxyId the id of proxy
     * @return NetCDFProxy for a given proxyId (i.e. proxy file name)
     */
    public NetCDFProxy getNetCDFProxy(String experimentAccession, String proxyId) throws IOException {
        return new NetCDFProxy(new File(getDataDirectory(experimentAccession), proxyId));
    }


    /**
     * @param experimentAccession the experiment to find proxy for
     * @param experimentId
     * @param arrayDesignAcc      Array Design accession
     * @param geneIds             data for these gene ids is required to exist in the found proxy
     * @return if arrayDesignAcc != null, id of first proxy for experimentAccession, that matches arrayDesignAcc;
     *         otherwise, id of first proxy in the list returned by getNetCDFProxiesForExperiment()
     */
    private NetCDFDescriptor findNetcdf(final String experimentAccession, final long experimentId, final String arrayDesignAcc, final Set<Long> geneIds) throws IOException {
        for (NetCDFDescriptor ncdf : getNetCDFProxiesForExperiment(experimentAccession, experimentId)) {
            NetCDFProxy proxy = null;
            try {
                proxy = ncdf.createProxy();

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
                if (isNullOrEmpty(arrayDesignAcc)) {
                    if (geneIds == null || geneIds.isEmpty() || geneIdsInProxy == null || geneIdsInProxy.containsAll(geneIds)) {
                        return ncdf;
                    }
                } else {
                    if (arrayDesignAcc.equals(adAcc)) {
                        return ncdf;
                    }
                }
            } finally {
                closeQuietly(proxy);
            }
        }
        return null;
    }

    /**
     * @param experimentAccession
     * @param experimentId
     * @return all ncdf files corresponding to experimentAccession and experimentId
     * @throws RuntimeException if at least one ncdf file in experimentAccession's directory does not start with experimentId
     */
    public File[] listNetCDFs(String experimentAccession, long experimentId) {
        File[] list = getDataDirectory(experimentAccession).listFiles(extension("nc", false));
        if (list == null) {
            return new File[0];
        } else {

            List<String> incorrectExperimentIdNcdfs = new ArrayList<String>();
            for (final File netCDF : list) {
                if (!netCDF.getAbsolutePath().matches("^.*" + experimentId + "\\_[\\d]+\\.nc$")) {
                    incorrectExperimentIdNcdfs.add(netCDF.getAbsolutePath());
                }
            }
            if (incorrectExperimentIdNcdfs.size() > 0) {
                throw logUnexpected("The following ncdfs did not match experiment id: " + experimentId + " for: " + experimentAccession + ": " + incorrectExperimentIdNcdfs);
            }
        }
        return list;
    }

    public File getDataDirectory(String experimentAccession) {
        final String[] parts = experimentAccession.split("-");
        if (parts.length != 3 || !"E".equals(parts[0])) {
            throw logUnexpected("Invalid experiment accession: " + experimentAccession);
        }
        final String num = (parts[2].length() > 2) ?
                parts[2].substring(0, parts[2].length() - 2) + "00" : "00";
        return new File(new File(new File(atlasDataRepo, parts[1]), num), experimentAccession);
    }

    /**
     * @param experimentAccession experiment to get proxies for
     * @param experimentId
     * @return List of NetCDF proxies corresponding to experimentAccession
     */
    private Collection<NetCDFDescriptor> getNetCDFProxiesForExperiment(final String experimentAccession, final long experimentId) throws IOException {
        // lookup NetCDFFiles for this experiment
        File[] netCDFs = listNetCDFs(experimentAccession, experimentId);

        List<NetCDFDescriptor> nsdfs = new ArrayList<NetCDFDescriptor>(netCDFs.length);
        for (File netCDF : netCDFs) {
            nsdfs.add(new NetCDFDescriptor(netCDF));
        }

        return nsdfs;
    }

    /**
     * @param proxy
     * @return List of geneIds in proxy
     * @throws IOException
     */
    private List<Long> getGeneIds(final NetCDFProxy proxy) throws IOException {
        return asList(proxy.getGenes());
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
     * @param experimentId
     * @param geneId
     * @param ef
     * @param efv
     * @param isUp
     * @return best (UP if isUp == true; DOWN otherwise) ExpressionAnalysis for geneId-ef-efv in experimentAccession's
     *         first proxy in which expression data for that combination exists
     */
    public ExpressionAnalysis getBestEAForGeneEfEfvInExperiment(final String experimentAccession,
                                                                final long experimentId,
                                                                final Long geneId,
                                                                final String ef,
                                                                final String efv,
                                                                final boolean isUp) {
        ExpressionAnalysis ea = null;
        try {
            Collection<NetCDFDescriptor> ncdfs = getNetCDFProxiesForExperiment(experimentAccession, experimentId);
            for (NetCDFDescriptor ncdf : ncdfs) {
                NetCDFProxy proxy = null;
                try {
                    proxy = ncdf.createProxy();
                    if (ea == null) {
                        Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(proxy, singleton(geneId));
                        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                                proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes, ef, efv, isUp);
                        if (geneIdsToEfToEfvToEA.containsKey(geneId) &&
                                geneIdsToEfToEfvToEA.get(geneId).containsKey(ef) &&
                                geneIdsToEfToEfvToEA.get(geneId).get(ef).containsKey(efv) &&

                                geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv) != null) {
                            ea = geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv);
                        }

                    }
                } finally {
                    closeQuietly(proxy);
                }
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

        NetCDFProxy proxy = null;
        try {
            proxy = getNetCDFProxy(experimentAccession, proxyId);
            Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(proxy, singleton(geneId));
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes);
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
     * @param dir the directory to search NetCDFs in
     * @return List of all NetCDF Files in dir
     */
    private List<File> getAllNcdfs(File dir) {
        List<File> ncdfs = new ArrayList<File>();
        ncdfs.addAll(Arrays.asList(dir.listFiles(extension("nc", false))));

        // We assume as soon as there are NetCDF files in the directory,
        // there's no point looking deeper in the file hierarchy
        if (ncdfs.isEmpty()) {
            List<File> files = Arrays.asList(dir.listFiles());
            for (File file : files) {
                if (file.isDirectory())
                    ncdfs.addAll(getAllNcdfs(file));
            }
        }
        return ncdfs;
    }

    public NetCDFDescriptor getNetCdfFile(String experimentAccession, long experimentId, String arrayDesignAccession, Set<Long> geneIds) {
        try {
            return findNetcdf(experimentAccession, experimentId, arrayDesignAccession, geneIds);
        } catch (IOException e) {
            return null;
        }
    }

    public List<String> getFactorValues(String experimentAccession, String proxyId, String ef) throws IOException {
        NetCDFProxy proxy = null;
        try {
            proxy = getNetCDFProxy(experimentAccession, proxyId);
            return Arrays.asList(proxy.getFactorValues(ef));
        } finally {
            closeQuietly(proxy);
        }
    }
}

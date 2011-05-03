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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.Model;
import uk.ac.ebi.gxa.impl.ModelImpl.DataAccessor;
import uk.ac.ebi.gxa.utils.ZipUtil;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Expression;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;
import static com.google.common.primitives.Floats.asList;
import static java.util.Collections.singleton;
import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;
import static uk.ac.ebi.gxa.netcdf.reader.NetCDFPredicates.containsGenes;
import static uk.ac.ebi.gxa.utils.FileUtil.extension;

/**
 * This class wraps the functionality of retrieving values across multiple instances of NetCDFProxy
 *
 * @author Alexey Filippov
 * @author Rober Petryszak
 * @author Nikolay Pultsin
 */
public class AtlasNetCDFDAO implements DataAccessor {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Location of the experiment data files
    private File atlasDataRepo;

    private Model atlasModel;

    public void setAtlasModel(Model atlasModel) {
        this.atlasModel = atlasModel;
    }

    private static String getFilename(Experiment experiment, ArrayDesign arrayDesign) {
        return experiment.getId() + "_" + arrayDesign.getArrayDesignID() + ".nc";
    }

    public File getNetCDFLocation(Experiment experiment, ArrayDesign arrayDesign) {
        return new File(getDataDirectory(experiment.getAccession()), getFilename(experiment, arrayDesign));
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
     * @param experimentAccession experiment to plot
     * @param geneIds             ids of genes to plot
     * @param criteria            other criteria to choose NetCDF to plot
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws IOException in case of I/O errors
     */
    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(
            @Nonnull final String experimentAccession, @Nonnull Collection<Long> geneIds, @Nonnull Predicate<NetCDFProxy> criteria) throws IOException {
        final NetCDFDescriptor netCDF = findNetCDF(experimentAccession, Predicates.<NetCDFProxy>and(containsGenes(geneIds), criteria));
        if (netCDF == null)
            return null;

        NetCDFProxy proxy = null;
        try {
            proxy = netCDF.createProxy();
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
     * @param criteria            the criteria to choose NetCDF proxy
     * @return if arrayDesignAcc != null, id of first proxy for experimentAccession, that matches arrayDesignAcc;
     *         otherwise, id of first proxy in the list returned by getNetCDFProxiesForExperiment()
     */
    private NetCDFDescriptor findNetCDF(final String experimentAccession, Predicate<NetCDFProxy> criteria) throws IOException {
        for (NetCDFDescriptor ncdf : getNetCDFProxiesForExperiment(experimentAccession)) {
            NetCDFProxy proxy = null;
            try {
                proxy = ncdf.createProxy();
                if (criteria.apply(proxy)) {
                    return ncdf;
                }
            } finally {
                closeQuietly(proxy);
            }
        }
        return null;
    }

    /**
     * @param experimentAccession
     * @return all ncdf files corresponding to experimentAccession
     * @throws RuntimeException if at least one ncdf file in experimentAccession's directory does not start with experimentId
     */
    public File[] listNetCDFs(String experimentAccession) {
        final Experiment experiment = atlasModel.getExperimentByAccession(experimentAccession);
        File[] list = getDataDirectory(experimentAccession).listFiles(extension("nc", false));
        if (list == null) {
            return new File[0];
        } else {
            List<String> incorrectExperimentIdNcdfs = new ArrayList<String>();
            for (final File netCDF : list) {
                if (!netCDF.getAbsolutePath().matches("^.*" + experiment.getId() + "\\_[\\d]+\\.nc$")) {
                    incorrectExperimentIdNcdfs.add(netCDF.getAbsolutePath());
                }
            }
            if (incorrectExperimentIdNcdfs.size() > 0) {
                throw logUnexpected("The following ncdfs did not match experiment id: " + experiment.getId() + " for: " + experimentAccession + ": " + incorrectExperimentIdNcdfs);
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
     * @return List of NetCDF proxies corresponding to experimentAccession
     */
    public Collection<NetCDFDescriptor> getNetCDFProxiesForExperiment(final String experimentAccession)  {
        // lookup NetCDFFiles for this experiment
        File[] netCDFs = listNetCDFs(experimentAccession);

        List<NetCDFDescriptor> nsdfs = new ArrayList<NetCDFDescriptor>(netCDFs.length);
        for (File netCDF : netCDFs) {
            nsdfs.add(new NetCDFDescriptor(netCDF));
        }

        return nsdfs;
    }

    /**
     * @param proxy
     * @param geneIds
     * @return Map: geneId -> List of design element indexes in proxy
     * @throws IOException
     */
    private Map<Long, List<Integer>> getGeneIdToDesignElementIndexes(
            final NetCDFProxy proxy,
            final Collection<Long> geneIds)
            throws IOException {
        // Note that in a given NetCDF proxy more than one geneIndex (==designElementIndex) may correspond to one geneId
        // (i.e. proxy.getGenes() may contain duplicates, whilst proxy.getDesignElements() will not; and
        // proxy.getGenes().size() == proxy.getDesignElements().size())
        Map<Long, List<Integer>> geneIdToDEIndexes = new HashMap<Long, List<Integer>>();

        int deIndex = 0;
        for (Long geneId : proxy.getGeneIds()) {
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
     * @param expression
     * @return best (according to expression) ExpressionAnalysis for geneId-ef-efv in experimentAccession's
     *         first proxy in which expression data for that combination exists
     */
    public ExpressionAnalysis getBestEAForGeneEfEfvInExperiment(final String experimentAccession,
                                                                final Long geneId,
                                                                final String ef,
                                                                final String efv,
                                                                final Expression expression) {
        ExpressionAnalysis ea = null;
        try {
            Collection<NetCDFDescriptor> ncdfs = getNetCDFProxiesForExperiment(experimentAccession);
            for (NetCDFDescriptor ncdf : ncdfs) {
                NetCDFProxy proxy = null;
                try {
                    proxy = ncdf.createProxy();
                    if (ea == null) {
                        Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(proxy, singleton(geneId));
                        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                                proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes, ef, efv, expression);
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

    public NetCDFDescriptor getNetCdfFile(String experimentAccession, Predicate<NetCDFProxy> criteria) {
        try {
            return findNetCDF(experimentAccession, criteria);
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

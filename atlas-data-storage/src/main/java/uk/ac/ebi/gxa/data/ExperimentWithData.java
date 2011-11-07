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

package uk.ac.ebi.gxa.data;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.primitives.Floats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.CollectionUtil;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.util.*;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class ExperimentWithData implements Closeable {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AtlasDataDAO atlasDataDAO;
    private final Experiment experiment;

    private final Map<ArrayDesign, DataProxy> proxies = new HashMap<ArrayDesign, DataProxy>();

    // cached data
    private final Map<ArrayDesign, String[]> designElementAccessions = new HashMap<ArrayDesign, String[]>();

    ExperimentWithData(@Nonnull AtlasDataDAO atlasDataDAO, @Nonnull Experiment experiment) {
        this.atlasDataDAO = atlasDataDAO;
        this.experiment = experiment;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    /**
     * @param criteria the criteria to choose arrayDesign
     * @return first arrayDesign used in experiment, that matches criteria;
     *         or null if no arrayDesign has been found
     */
    public ArrayDesign findArrayDesign(Predicate<ArrayDesign> criteria) {
        for (ArrayDesign ad : experiment.getArrayDesigns()) {
            if (criteria.apply(ad)) {
                return ad;
            }
        }
        return null;
    }

    DataProxy getProxy(ArrayDesign arrayDesign) throws AtlasDataException {
        DataProxy p = proxies.get(arrayDesign);
        if (p == null) {
            p = atlasDataDAO.createDataProxy(experiment, arrayDesign);
            proxies.put(arrayDesign, p);
        }
        return p;
    }

    public NetCDFDataCreator getDataCreator(ArrayDesign arrayDesign) {
        return new NetCDFDataCreator(atlasDataDAO, experiment, arrayDesign);
    }

    /**
     * Updates data files according to the {@link ArrayDesign} supplied.
     * <p/>
     * Please note that due to duality of the DAO layer {@link ArrayDesign}
     * may or may not contain design elements depending on how you got it.
     * <p/>
     * This is indeed a problem which should be addressed ASAP.
     * <p/>
     * TODO: check whether arrayDesign is shallow, and load full one if it is.
     *
     * @param arrayDesign array design (with DEs) to update data files for
     */
    public void updateData(ArrayDesign arrayDesign) throws AtlasDataException {
        new DataUpdater().update(arrayDesign);
    }

    public void updateDataToNewestVersion() throws AtlasDataException {
        for (ArrayDesign arrayDesign : experiment.getArrayDesigns()) {
            final DataProxy proxy = getProxy(arrayDesign);
            if ("2.0".equals(proxy.getVersion())) {
                return;
            }

            try {
                new DataUpdater().update(arrayDesign);
            } finally {
                closeQuietly(proxy);
                proxies.remove(arrayDesign);
            }
        }
    }

    public NetCDFStatisticsCreator getStatisticsCreator(ArrayDesign arrayDesign) {
        return new NetCDFStatisticsCreator(atlasDataDAO, experiment, arrayDesign);
    }

    /*
     * This method returns assays in the order they are stored in netcdf file.
     * While this order is important we have to use this method,
     * in future it would be replaced by Experiment method.
     */
    public List<Assay> getAssays(ArrayDesign arrayDesign) throws AtlasDataException {
        final String[] assayAccessions = getProxy(arrayDesign).getAssayAccessions();

        final Map<String, Assay> experimentAssays = new HashMap<String, Assay>();
        for (Assay a : experiment.getAssaysForDesign(arrayDesign)) {
            experimentAssays.put(a.getAccession(), a);
        }
        if (assayAccessions.length != experimentAssays.size()) {
            throw new AtlasDataException("Experiment " + experiment.getAccession() + "/" + arrayDesign.getAccession() + " contains " + experimentAssays.size() + " assays but data storage contains " + assayAccessions.length);
        }

        final ArrayList<Assay> assays = new ArrayList<Assay>(assayAccessions.length);
        for (String accession : assayAccessions) {
            final Assay a = experimentAssays.get(accession);
            if (a == null) {
                throw new AtlasDataException("Experiment " + experiment.getAccession() + "/" + arrayDesign.getAccession() + " does not contain an assay with accession " + accession + " that is mentioned in data storage");
            }
            assays.add(a);
        }
        return assays;
    }

    public int[][] getSamplesToAssays(ArrayDesign arrayDesign) throws AtlasDataException {
        return getProxy(arrayDesign).getSamplesToAssays();
    }

    public String[] getDesignElementAccessions(ArrayDesign arrayDesign) throws AtlasDataException {
        String[] array = designElementAccessions.get(arrayDesign);
        if (array == null) {
            array = getProxy(arrayDesign).getDesignElementAccessions();
            designElementAccessions.put(arrayDesign, array);
        }
        return array;
    }

    public long[] getGenes(ArrayDesign arrayDesign) throws AtlasDataException {
        return getProxy(arrayDesign).getGenes();
    }

    public List<KeyValuePair> getUniqueFactorValues(ArrayDesign arrayDesign) throws AtlasDataException, StatisticsNotFoundException {
        List<KeyValuePair> uniqueEFVs = new ArrayList<KeyValuePair>();
        List<String> factors = Arrays.asList(getFactors(arrayDesign));

        for (KeyValuePair propVal : getUniqueValues(arrayDesign)) {
            if (factors.contains(propVal.key)) {
                // Since getUniqueValues() returns both ef-efvs/sc-scvs, filter out scs that aren't also efs
                uniqueEFVs.add(propVal);
            }
        }
        return uniqueEFVs;
    }

    public List<KeyValuePair> getUniqueValues(ArrayDesign arrayDesign) throws AtlasDataException, StatisticsNotFoundException {
        return getProxy(arrayDesign).getUniqueValues();
    }

    public String[] getFactors(ArrayDesign arrayDesign) throws AtlasDataException {
        return getProxy(arrayDesign).getFactors();
    }

    public String[] getCharacteristics(ArrayDesign arrayDesign) throws AtlasDataException {
        return getProxy(arrayDesign).getCharacteristics();
    }

    public String[] getCharacteristicValues(ArrayDesign arrayDesign, String characteristic) throws AtlasDataException {
        return getProxy(arrayDesign).getCharacteristicValues(characteristic);
    }

    public String[][] getFactorValues(ArrayDesign arrayDesign) throws AtlasDataException {
        return getProxy(arrayDesign).getFactorValues();
    }

    public String[] getFactorValues(ArrayDesign arrayDesign, String factor) throws AtlasDataException {
        return getProxy(arrayDesign).getFactorValues(factor);
    }

    public FloatMatrixProxy getExpressionValues(ArrayDesign arrayDesign, int[] deIndices) throws AtlasDataException {
        return getProxy(arrayDesign).getExpressionValues(deIndices);
    }

    public float[] getExpressionDataForDesignElementAtIndex(ArrayDesign arrayDesign, int designElementIndex) throws AtlasDataException {
        return getProxy(arrayDesign).getExpressionDataForDesignElementAtIndex(designElementIndex);
    }

    /**
     * Extracts expression statistic values for given design element indices.
     *
     * @param deIndices an array of design element indices to extract expression statistic for
     * @return an instance of {@link ExpressionStatistics}
     * @throws AtlasDataException if the data could not be read from the netCDF file
     *                            or if array of design element indices contains out of bound indices
     */
    public ExpressionStatistics getExpressionStatistics(ArrayDesign arrayDesign, int[] deIndices) throws AtlasDataException, StatisticsNotFoundException {
        return ExpressionStatistics.create(deIndices, getProxy(arrayDesign));
    }

    private List<ExpressionAnalysis> getAllExpressionAnalyses(ArrayDesign arrayDesign, int deIndex) throws AtlasDataException, StatisticsNotFoundException {
        return getExpressionAnalysesByFactor(arrayDesign, deIndex, null, null);
    }

    /**
     * @param arrayDesign ArrayDesign to search in
     * @param deIndex     index of DE of interest
     * @param efName      name of EF to search for (<code>null</code> for all EFs)
     * @param efvName     value of EF to search for
     * @return list of ExpressionAnalyses for given ArrayDesign and ef/efv pair if ef is not null
     *         list of all ExpressionAnalyses for given ArrayDesign otherwise
     * @throws AtlasDataException          if data files are not found, broken or otherwise cause an error reading
     * @throws StatisticsNotFoundException if statistics is not ready
     */
    private List<ExpressionAnalysis> getExpressionAnalysesByFactor(
            ArrayDesign arrayDesign, int deIndex,
            @Nullable String efName, @Nullable String efvName) throws AtlasDataException, StatisticsNotFoundException {
        final String deAccession = getDesignElementAccessions(arrayDesign)[deIndex];
        final float[] p = getPValuesForDesignElement(arrayDesign, deIndex);
        final float[] t = getTStatisticsForDesignElement(arrayDesign, deIndex);

        final List<KeyValuePair> uniqueValues = getUniqueValues(arrayDesign);

        final List<ExpressionAnalysis> result = new ArrayList<ExpressionAnalysis>();
        for (int efIndex = 0; efIndex < p.length; efIndex++) {
            final KeyValuePair uniqueValue = uniqueValues.get(efIndex);
            if (efName == null ||
                    (uniqueValue.key.equals(efName) && uniqueValue.value.equals(efvName))) {
                result.add(new ExpressionAnalysis(
                        arrayDesign.getAccession(),
                        deAccession,
                        deIndex,
                        uniqueValue.key,
                        uniqueValue.value,
                        t[efIndex],
                        p[efIndex]
                ));
            }
        }
        return result;
    }

    /**
     * For each gene in the keySet() of geneIdsToDEIndexes, and each efv in uniqueValues,
     * find the design element with a minPvalue and store it as an ExpressionAnalysis object in
     * geneIdsToEfToEfvToEA if the minPvalue found in this proxy is better than the one already in
     * geneIdsToEfToEfvToEA.
     *
     * @param geneIdsToDEIndexes geneId -> list of design element indexes containing data for that gene
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws AtlasDataException in case of I/O errors
     */
    private Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForDesignElementIndexes(
            ArrayDesign arrayDesign,
            Map<Long, List<Integer>> geneIdsToDEIndexes
    ) throws AtlasDataException, StatisticsNotFoundException {
        return getExpressionAnalysesForDesignElementIndexes(arrayDesign, geneIdsToDEIndexes, null, null, UpDownCondition.CONDITION_ANY);
    }

    /**
     * For each gene in the keySet() of geneIdsToDEIndexes,  and for either efVal-efvVal or (if both arguments are not null)
     * for each efv in uniqueValues, find the design element with a minPvalue and store it as an ExpressionAnalysis object in
     * geneIdsToEfToEfvToEA - if the minPvalue found in this proxy is better than the one already in
     * geneIdsToEfToEfvToEA.
     *
     * @param geneIdsToDEIndexes geneId -> list of design element indexes containing data for that gene
     * @param efVal              ef to retrieve ExpressionAnalyses for
     * @param efvVal             efv to retrieve ExpressionAnalyses for; if either efVal or efvVal are null,
     *                           ExpressionAnalyses for all ef-efvs will be retrieved
     * @param upDownCondition    desired expression; used only when efVal-efvVal are specified
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws AtlasDataException in case of I/O errors
     */
    private Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForDesignElementIndexes(
            ArrayDesign arrayDesign,
            final Map<Long, List<Integer>> geneIdsToDEIndexes,
            @Nullable final String efVal,
            @Nullable final String efvVal,
            final UpDownCondition upDownCondition
    ) throws AtlasDataException, StatisticsNotFoundException {
        final Map<Long, Map<String, Map<String, ExpressionAnalysis>>> result = newHashMap();

        for (Map.Entry<Long, List<Integer>> entry : geneIdsToDEIndexes.entrySet()) {
            final Long geneId = entry.getKey();

            if (geneId == 0) continue; // skip geneid = 0

            final Map<String, Map<String, ExpressionAnalysis>> resultForGene = newHashMap();
            result.put(geneId, resultForGene);

            for (Integer deIndex : entry.getValue()) {
                List<ExpressionAnalysis> eaList = new ArrayList<ExpressionAnalysis>();
                if (efVal != null && efvVal != null) {
                    final List<ExpressionAnalysis> eas =
                            getExpressionAnalysesByFactor(arrayDesign, deIndex, efVal, efvVal);
                    if (!eas.isEmpty()) {
                        if (eas.size() == 1) {
                            final ExpressionAnalysis analysis = eas.get(0);
                            if (upDownCondition.apply(analysis.getUpDownExpression())) {
                                eaList.add(analysis);
                            }
                        } else
                            throw createUnexpected("We always expect eas to be either empty or of size == 1");
                    }
                } else {
                    eaList.addAll(getAllExpressionAnalyses(arrayDesign, deIndex));
                }

                for (ExpressionAnalysis ea : eaList) {
                    final String ef = ea.getEfName();
                    final String efv = ea.getEfvName();

                    Map<String, ExpressionAnalysis> resultForFactor = resultForGene.get(ef);
                    if (resultForFactor == null) {
                        resultForFactor = new HashMap<String, ExpressionAnalysis>();
                        resultForGene.put(ef, resultForFactor);
                    }

                    ExpressionAnalysis prevBestPValueEA = resultForFactor.get(efv);
                    if ((prevBestPValueEA == null ||
                            // Mo stats were available in the previously seen ExpressionAnalysis
                            Float.isNaN(prevBestPValueEA.getPValAdjusted()) || Float.isNaN(prevBestPValueEA.getTStatistic()) ||
                            // Stats are available for ea, an it has a better pValue than the previous  ExpressionAnalysis
                            (!Float.isNaN(ea.getPValAdjusted()) && prevBestPValueEA.getPValAdjusted() > ea.getPValAdjusted()) ||
                            // Stats are available for ea, both pValues are equals, then the better one is the one with the higher absolute tStat
                            (!Float.isNaN(ea.getPValAdjusted()) && !Float.isNaN(ea.getTStatistic()) &&
                                    prevBestPValueEA.getPValAdjusted() == ea.getPValAdjusted() &&
                                    Math.abs(prevBestPValueEA.getTStatistic()) < Math.abs(ea.getTStatistic())))
                            ) {
                        if (ea.getPValAdjusted() > 1) {
                            // As the NA pvals/tstats  currently come back from ncdfs as 1.0E30, we convert them to Float.NaN
                            ea.setPValAdjusted(Float.NaN);
                            ea.setTStatistic(Float.NaN);

                        }
                        resultForFactor.put(efv, ea);
                    }
                }
            }
        }
        return result;
    }

    /**
     * @param ad
     * @param geneIds
     * @return Map: geneId -> List of design element indexes for ArrayDesign
     * @throws AtlasDataException
     */
    private Map<Long, List<Integer>> getGeneIdToDesignElementIndexes(ArrayDesign ad, final Collection<Long> geneIds) throws AtlasDataException {
        // Note that in a given NetCDF proxy more than one geneIndex (==designElementIndex) may correspond to one geneId
        // (i.e. proxy.getGenes() may contain duplicates, whilst proxy.getDesignElements() will not; and
        // proxy.getGenes().size() == proxy.getDesignElements().size())
        Map<Long, List<Integer>> geneIdToDEIndexes = new HashMap<Long, List<Integer>>();

        int deIndex = 0;
        for (Long geneId : getGenes(ad)) {
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
     * @param geneIds  ids of genes to plot
     * @param criteria other criteria to choose NetCDF to plot
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains arrayDesign and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws AtlasDataException in case of I/O errors
     */
    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(@Nonnull Collection<Long> geneIds, @Nonnull Predicate<ArrayDesign> criteria) throws AtlasDataException, StatisticsNotFoundException {
        final ArrayDesign arrayDesign = findArrayDesign(Predicates.<ArrayDesign>and(new DataPredicates(this).containsGenes(geneIds), criteria));
        if (arrayDesign == null) {
            return null;
        }

        final Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(arrayDesign, geneIds);
        return getExpressionAnalysesForDesignElementIndexes(arrayDesign, geneIdToDEIndexes);
    }

    public float[] getPValuesForDesignElement(ArrayDesign arrayDesign, int designElementIndex) throws AtlasDataException, StatisticsNotFoundException {
        return getProxy(arrayDesign).getPValuesForDesignElement(designElementIndex);
    }

    public float[] getTStatisticsForDesignElement(ArrayDesign arrayDesign, int designElementIndex) throws AtlasDataException, StatisticsNotFoundException {
        return getProxy(arrayDesign).getTStatisticsForDesignElement(designElementIndex);
    }

    public TwoDFloatArray getAllExpressionData(ArrayDesign arrayDesign) throws AtlasDataException {
        return getProxy(arrayDesign).getAllExpressionData();
    }

    public TwoDFloatArray getTStatistics(ArrayDesign arrayDesign) throws AtlasDataException, StatisticsNotFoundException {
        return getProxy(arrayDesign).getTStatistics();
    }

    public TwoDFloatArray getPValues(ArrayDesign arrayDesign) throws AtlasDataException, StatisticsNotFoundException {
        return getProxy(arrayDesign).getPValues();
    }

    /**
     * @param geneId
     * @param ef
     * @param efv
     * @param upDownCondition
     * @return best (according to expression) ExpressionAnalysis for geneId-ef-efv in experimentAccession's
     *         first proxy in which expression data for that combination exists
     */
    // TODO: remove this method from ExperimentWithData or throw AtlasDataException & StatisticsNotFoundException outside
    public ExpressionAnalysis getBestEAForGeneEfEfvInExperiment(Long geneId, String ef, String efv, UpDownCondition upDownCondition) {
        ExpressionAnalysis ea = null;
        try {
            final Collection<ArrayDesign> ads = experiment.getArrayDesigns();
            for (ArrayDesign ad : ads) {
                if (ea == null) {
                    Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(ad, Collections.singleton(geneId));
                    Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                            getExpressionAnalysesForDesignElementIndexes(ad, geneIdToDEIndexes, ef, efv, upDownCondition);
                    if (geneIdsToEfToEfvToEA.containsKey(geneId) &&
                            geneIdsToEfToEfvToEA.get(geneId).containsKey(ef) &&
                            geneIdsToEfToEfvToEA.get(geneId).get(ef).containsKey(efv) &&

                            geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv) != null) {
                        ea = geneIdsToEfToEfvToEA.get(geneId).get(ef).get(efv);
                    }

                }
            }
        } catch (StatisticsNotFoundException e) {
            log.error("Failed to ExpressionAnalysis for gene id: " + geneId + "; ef: " + ef + " ; efv: " + efv + " in experiment: " + experiment);
        } catch (AtlasDataException e) {
            log.error("Failed to ExpressionAnalysis for gene id: " + geneId + "; ef: " + ef + " ; efv: " + efv + " in experiment: " + experiment);
        }
        return ea;
    }

    /**
     * @param arrayDesign
     * @param geneId
     * @param ef
     * @return Map: efv -> best ExpressionAnalysis for geneid-ef in this proxy
     * @throws AtlasDataException
     */
    public Map<String, ExpressionAnalysis> getBestEAsPerEfvInProxy(ArrayDesign arrayDesign, Long geneId, String ef) throws AtlasDataException, StatisticsNotFoundException {
        Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(arrayDesign, Collections.singleton(geneId));
        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                getExpressionAnalysesForDesignElementIndexes(arrayDesign, geneIdToDEIndexes);
        return geneIdsToEfToEfvToEA.get(geneId).get(ef);
    }

    public String getDataPathForR(ArrayDesign arrayDesign) {
        return atlasDataDAO.getDataFile(experiment, arrayDesign).getAbsolutePath();
    }

    public String getStatisticsPathForR(ArrayDesign arrayDesign) {
        return atlasDataDAO.getStatisticsFile(experiment, arrayDesign).getAbsolutePath();
    }

    public void close() {
        for (DataProxy p : proxies.values())
            closeQuietly(p);
        proxies.clear();
    }

    private class DataUpdater {
        void update(ArrayDesign arrayDesign) throws AtlasDataException {
            log.info("Reading existing NetCDF for {} / {}", experiment.getAccession(), arrayDesign.getAccession());
            final NetCDFData data = readNetCDF(arrayDesign);

            log.info("Writing updated NetCDF for {} / {}", experiment.getAccession(), arrayDesign.getAccession());
            writeNetCDF(data, arrayDesign);

            log.info("Successfully updated NetCDF for {} / {}", experiment.getAccession(), arrayDesign.getAccession());

            final boolean removeObsoleteNetcdf = getProxy(arrayDesign) instanceof NetCDFProxyV1;
            if (removeObsoleteNetcdf) {
                log.info("Dropping old NetCDF for {} / {}", experiment.getAccession(), arrayDesign.getAccession());
                File v1File = atlasDataDAO.getV1File(experiment, arrayDesign);
                if (!v1File.delete()) {
                    log.warn("Cannot delete old NetCDF: {}", v1File);
                    throw new AtlasDataException("Cannot delete old NetCDF: " + v1File);
                }
            }
        }

        private NetCDFData readNetCDF(ArrayDesign arrayDesign) throws AtlasDataException {
            final NetCDFData data = new NetCDFData();
            final List<Integer> usedAssays = new LinkedList<Integer>();
            int index = 0;
            // WARNING: getAssays() list assays in the order they are listed in netcdf file
            for (Assay assay : getAssays(arrayDesign)) {
                data.addAssay(assay);
                usedAssays.add(index);
                ++index;
            }

            final String[] deAccessions = getDesignElementAccessions(arrayDesign);
            data.setStorage(new DataMatrixStorage(data.getWidth(), deAccessions.length, 1));
            for (int i = 0; i < deAccessions.length; ++i) {
                final float[] values = getExpressionDataForDesignElementAtIndex(arrayDesign, i);
                data.addToStorage(deAccessions[i], CollectionUtil.multiget(Floats.asList(values), usedAssays).iterator());
            }
            return data;
        }

        private void writeNetCDF(NetCDFData data, ArrayDesign arrayDesign) throws AtlasDataException {
            final NetCDFDataCreator dataCreator = getDataCreator(arrayDesign);

            dataCreator.setAssayDataMap(data.getAssayDataMap());
            // TODO: Decide if statistics should be migrated on update. Right now this is not done: stats will be empty.

            dataCreator.createNetCdf();

            log.info("Successfully finished NetCDF for " + experiment.getAccession() + " and " + arrayDesign.getAccession());
        }
    }
}

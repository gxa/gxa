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

package uk.ac.ebi.gxa.web;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasGene;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;
import uk.ac.ebi.gxa.utils.*;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class AtlasPlotter {
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private AtlasDAO atlasDatabaseDAO;
    private AtlasSolrDAO atlasSolrDAO;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String[] altColors = {"#D8D8D8", "#E8E8E8"};
    private static final String[] markingColors = {"#F0FFFF", "#F5F5DC"};
    private static final Pattern startsOrEndsWithDigits = java.util.regex.Pattern.compile("^\\d+|\\d+$");
    // This constant is used to prevent empty efvs from being displayed in plots (cf. SDRFWritingUtils)
    private static final String EMPTY_EFV = "(empty)";

    // Maximum of plotted expression data per factor - used for restricting the amount of displayed data points in large plots
    private static final int MAX_DATAPOINTS_PER_ASSAY = 500;

    public AtlasDAO getAtlasDatabaseDAO() {
        return atlasDatabaseDAO;
    }

    public void setAtlasDatabaseDAO(AtlasDAO atlasDatabaseDAO) {
        this.atlasDatabaseDAO = atlasDatabaseDAO;
    }

    public AtlasSolrDAO getAtlasSolrDAO() {
        return atlasSolrDAO;
    }

    public void setAtlasSolrDAO(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }


    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public Map<String,Object> getGeneInExpPlotData(final String geneIdKey,
                                    final String experimentID,
                                    final String ef,
                                    final String efv,
                                    final String plotType) {

        log.debug("Plotting gene {}, experiment {}, factor {}", new Object[]{geneIdKey, experimentID, ef});
        NetCDFProxy proxy = null;
        try {
            List<AtlasGene> genes = new ArrayList<AtlasGene>();
            Set<Long> geneIds = new LinkedHashSet<Long>();

            // lookup gene names, again using SOLR index
            for(String geneIdStr : geneIdKey.split(",")) {
                AtlasSolrDAO.AtlasGeneResult gene = getAtlasSolrDAO().getGeneById(geneIdStr);
                if (gene.isFound()) {
                    AtlasGene atlasGene = gene.getGene();
                    genes.add(atlasGene);
                    geneIds.add(Long.parseLong(atlasGene.getGeneId()));
                }
            }

            if(genes.isEmpty())
                throw new RuntimeException("No existing genes specified by query" + geneIdKey);

            // geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
            // Note that ea contains proxyId and designElement index from which it came, so that
            // the actual expression values can be easily r etrieved later
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    atlasNetCDFDAO.getExpressionAnalysesForGeneIds(geneIds, experimentID);

            String efToPlot;

            if ("default".equals(ef)) {
                Long geneId = Long.parseLong(genes.get(0).getGeneId());
                // First try to get the highest ranking from top gene
                efToPlot = getHighestRankEF(geneIdsToEfToEfvToEA.get(geneId));
                if (efToPlot == null) {
                    // if ef is "default" fetch highest ranked EF using SOLR index
                    efToPlot = genes.get(0).getHighestRankEF(Long.valueOf(experimentID)).getFirst();
                }
            } else {
                efToPlot = ef;
            }

            if (efToPlot == null)
                throw new RuntimeException("Can't find EF to plot");

            if (plotType.equals("thumb")) {
                AtlasGene geneToPlot = genes.get(0);
                Long geneId = Long.parseLong(geneToPlot.getGeneId());
                ExpressionAnalysis bestEA = geneIdsToEfToEfvToEA.get(geneId).get(efToPlot).get(efv);
                if (bestEA == null)
                    throw new RuntimeException("Can't find deIndex for min pValue for gene " + geneIdKey);
                return createThumbnailPlot(efToPlot, efv, bestEA);
            } else if (plotType.equals("large")) {
                proxy = atlasNetCDFDAO.getNetCDFProxy(getBestProxyId(geneIdsToEfToEfvToEA));
                assert(proxy != null); // At least one proxy for this experiment should contain geneIds
                Set<Long> geneIdsNotInBestProxy = getGenesNotInProxyIdForEf(geneIdsToEfToEfvToEA, proxy.getId(), efToPlot);

                List<AtlasGene> genesToPlot = new ArrayList<AtlasGene>();
                // if some genes in geneIds (thus in geneIdsToEfToEfvToEA) came from a different proxy than the
                // best proxy we found before, we'd get an exception when trying to obtain their expression data to
                // plot from the best proxy. As an example geneId = 130145002 (GH3.3) is one of top 10 genes for
                // experiment id = 596322149 (E-GEOD-1111), with 2 different ncdfs: 596322149_130140436.nc
                // and 596322149_130297520.nc. Best EA's for all the other top genes come from 596322149_130140436.nc,
                // but for GH3.3 it comes from 596322149_130297520.nc. Since the best (most frequent) proxy amongst top 10
                // genes is 596322149_130140436.nc, that is the one we choose - but that doesn't have any expression data
                // for GH3.3. Short term solution: remove geneIdsNotInBestProxy from genes (and thus from the plot).
                for (AtlasGene gene : genes) {
                    if (!geneIdsNotInBestProxy.contains(Long.parseLong(gene.getGeneId()))) {
                        genesToPlot.add(gene);
                    } else {
                        log.info("Excluding from plot gene: " + gene.getGeneId() + " (" + gene.getGeneName() + ") because its expression data for plotted factor: " + efToPlot + " could not be found in  the plotted proxy: " + proxy.getId());
                    }
                }
                return createLargePlot(proxy, efToPlot, genesToPlot, experimentID);
            } else {
                AtlasGene geneToPlot = genes.get(0);
                Long geneId = Long.parseLong(geneToPlot.getGeneId());
                Map<String, ExpressionAnalysis> efvToBestEA = geneIdsToEfToEfvToEA.get(geneId).get(efToPlot);
                return createBarPlot(geneId, efToPlot, efv, efvToBestEA, experimentID);
            }

        } catch (IOException e) {
            log.error("IOException whilst trying to read from NetCDFs at " + atlasNetCDFDAO.getAtlasNetCDFRepoPath() +
                    " for " + experimentID);
            throw new RuntimeException("IOException whilst trying to read from NetCDF for "
                    + atlasNetCDFDAO.getAtlasNetCDFRepoPath(), e);
        } finally {
            try {
                if (null != proxy) // proxy used for createLargePlot()
                    proxy.close();
            } catch (IOException e) {
                log.error("Failed to close NetCDF proxy: " + proxy.getId(), e);
            }
        }
    }


    /**
     * @param geneIdsToEfToEfvToEA // geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     * @return find proxy id which occurs most often in ExpressionAnalyses in geneIdsToEfToEfvToEA
     */
    private String getBestProxyId(Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA) {
        // One plot should be displayed per netCDF proxy (while one experiment can be stored in a number of
        // NetCDF files). However, since the ui is currently geared around displaying one plot only
        // per gene-experiment-ef-efv, we compromise by choosing the proxy which contains most of the
        // best ExpressionAnalyses in efvToBestEA.values().
        List<String> proxyIdsinBestEAs = new ArrayList<String>();
        for (Long geneId : geneIdsToEfToEfvToEA.keySet()) {
            Map<String, Map<String, ExpressionAnalysis>> efToEfvToEA = geneIdsToEfToEfvToEA.get(geneId);
            for (String ef : efToEfvToEA.keySet()) {
                Map<String, ExpressionAnalysis> efvToEA = efToEfvToEA.get(ef);
                proxyIdsinBestEAs.addAll(getProxyIds(efvToEA.values()));
            }
        }
        return getMostFrequentProxyId(proxyIdsinBestEAs);
    }

    /**
     *
     * @param geneIdsToEfToEfvToEA
     * @param proxyId
     * @param efToPlot
     * @return Set of geneIds whose best EA's for efToPlot are not in proxyId.
     */
    private Set<Long> getGenesNotInProxyIdForEf(
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA,
            String proxyId,
            String efToPlot) {
        Set<Long> geneIds = new HashSet<Long>();
        // One plot should be displayed per netCDF proxy (while one experiment can be stored in a number of
        // NetCDF files). However, since the ui is currently geared around displaying one plot only
        // per gene-experiment-ef-efv, we compromise by choosing the proxy which contains most of the
        // best ExpressionAnalyses in efvToBestEA.values().
        List<String> proxyIdsinBestEAs = new ArrayList<String>();
        for (Long geneId : geneIdsToEfToEfvToEA.keySet()) {
            Map<String, Map<String, ExpressionAnalysis>> efToEfvToEA = geneIdsToEfToEfvToEA.get(geneId);
            for (String ef : efToEfvToEA.keySet()) {
                if (ef.equals(efToPlot)) {
                    if (!getProxyIds(efToEfvToEA.get(ef).values()).contains(proxyId)) {
                        geneIds.add(geneId);
                    }
                }
            }
        }
        return geneIds;
    }

    /**
     * @param eas
     * @return find proxy id which occurs most often in ExpressionAnalyses in eas
     */
    private String getBestProxyId(Collection<ExpressionAnalysis> eas) {
        return getMostFrequentProxyId(getProxyIds(eas));
    }

    /**
     *
     * @param eas
     * @return list of proxy ids in eas
     */
    private List<String> getProxyIds(Collection<ExpressionAnalysis> eas) {
        List<String> proxyIdsInEAs = new ArrayList<String>();
        for (ExpressionAnalysis ea : eas) {
            proxyIdsInEAs.add(ea.getProxyId());
        }
        return proxyIdsInEAs;
    }

    /**
     *
     * @param proxyIds
     * @return the most frequently occurring proxy in proxyIds
     */
    private String getMostFrequentProxyId(List<String> proxyIds) {
        Set<String> uniqueProxyIds = new HashSet<String>(proxyIds);
        int bestProxyFreq = 0;
        String bestProxyId = null;
        for (String proxyId : uniqueProxyIds) {
            int freq = Collections.frequency(proxyIds, proxyId);
            if (freq > bestProxyFreq) {
                bestProxyId = proxyId;
                bestProxyFreq = freq;
            }
        }
        return bestProxyId;
    }


    /**
     * @param geneId
     * @param ef  experimental factor being plotted
     * @param efvClickedOn clicked on by the user on gene page. If non-null, its best Expression Analysis
     *        will determine which proxy to draw expression data from.
     * @param efvToBestEA Map: efv -> best EA, for this ef
     *                    All efv keys in this map will be plotted
     * @return Map key -> value representing a single plot
     * @throws IOException
     */
    private Map<String, Object> createBarPlot(
            Long geneId,
            String ef,
            String efvClickedOn,
            final Map<String, ExpressionAnalysis> efvToBestEA,
            final String experimentID)
            throws IOException {

        if (efvToBestEA.containsKey(EMPTY_EFV)) {
            // Don't plot (empty) efvs unless they are the only efv that could be plotted
            efvToBestEA.remove(EMPTY_EFV);
        }
        Set<String> efvsToPlot = efvToBestEA.keySet();

        log.debug("Creating plot... EF: {}, Top FVs: [{}], Best EAs: [{}]",
                new Object[]{ef, StringUtils.join(efvsToPlot, ","), efvToBestEA});

        List<Object> seriesList = new ArrayList<Object>();
        boolean insignificantSeries = false;
        // data for mean series
        List<List<Number>> meanSeriesData = new ArrayList<List<Number>>();

        // Map; proxy id to the factor values actually occuring in this proxy; used later
        // when plotting factors form the proxy in which bestEA (i.e lowest pValue) was found.
        Map<String, List<String>> proxyIdToFVs =
                atlasNetCDFDAO.getFactorValuesForExperiment(experimentID, ef);


        String bestProxyId = null;
        if (efvClickedOn != null && efvToBestEA.get(efvClickedOn) != null) {
            // If the user has clicked on an efv, choose to plot expression data from NetCDF proxy in which
            // the best pValue for this proxy occurred.
            bestProxyId = efvToBestEA.get(efvClickedOn).getProxyId();
        } else { // The user hasn't clicked on an efv - choose the proxy in most besEA across all efvs
            bestProxyId = getBestProxyId(efvToBestEA.values());
        }

        // Find array design accession for bestProxyId - this will be displayed under the plot
        String arrayDesignAcc = atlasNetCDFDAO.getArrayDesignAccession(bestProxyId);
        ArrayDesign arrayDesign = atlasDatabaseDAO.getArrayDesignByAccession(arrayDesignAcc);
        String arrayDesignDescription = arrayDesignAcc + " " + arrayDesign.getName();

        // Get unique factors from proxyId
        List<String> assayFVs = proxyIdToFVs.get(bestProxyId);
        Set<String> uniqueAssayFVs = new LinkedHashSet<String>();
        uniqueAssayFVs.addAll(assayFVs);
        if (uniqueAssayFVs.contains(EMPTY_EFV)) {
            uniqueAssayFVs.remove(EMPTY_EFV);
        }

        int counter = 0;
        int position = 0;
        // Find best pValue expressions for geneId and ef in bestProxyId - it's expression values for these
        // that will be plotted
        Map<String, ExpressionAnalysis> bestEAsPerEfvInProxy =
                atlasNetCDFDAO.getBestEAsPerEfvInProxy(bestProxyId, geneId, ef);

        for (String factorValue : uniqueAssayFVs) {
            ExpressionAnalysis bestEA = bestEAsPerEfvInProxy.get(factorValue);

            if (bestEA == null) {
                // If no bestEA expression analysis for factorValue could be found in proxy
                // (e.g. factorValue is present, but only with pVal == 0) then don't
                // plot this factorValue for proxyId
                continue;
            }
            // create a series for these datapoints - new series for each factor value
            List<Object> seriesData = new ArrayList<Object>();
            // Get the actual expression data from the proxy-designindex corresponding to the best pValue
            List<Float> expressions = atlasNetCDFDAO.getExpressionData(bestProxyId, bestEA.getDesignElementIndex());

            double meanForFV = 0;
            int meanCount = 0;

            int fvCount = 0;
            for (int assayIndex = 0; assayIndex < assayFVs.size(); assayIndex++) {
                if (assayFVs.get(assayIndex).equals(factorValue)) {
                    float value = expressions.get(assayIndex);
                    seriesData.add(Arrays.<Number>asList(++position, value <= -1000000 ? null : value));

                    if (value > -1000000) {
                        meanForFV += value;
                        ++meanCount;
                    }

                    ++fvCount;
                }
            }

            for (meanForFV /= meanCount; fvCount > 0; --fvCount) {
                meanSeriesData.add(Arrays.<Number>asList(meanSeriesData.size() + 1, meanForFV <= -1000000 ? null : meanForFV));
            }

            // store the data for this factor value series
            Map<String, Object> series = makeMap(
                    "data", seriesData,
                    // and store some other standard params
                    "bars", makeMap("show", true, "align", "center", "fill", true),
                    "lines", makeMap("show", false),
                    "points", makeMap("show", false),
                    "label", factorValue,
                    "legend", makeMap("show", true)
            );

            series.put("pvalue", bestEA.getPValAdjusted());
            series.put("expression", (bestEA.isUp() ? "up" : (bestEA.isNo() ? "no" : "dn")));

            // choose alternate series color for any insignificant factor values
            if (!efvsToPlot.contains(factorValue)) {
                series.put("color", altColors[counter % 2]);
                series.put("legend", makeMap("show", false));
                counter++;
                insignificantSeries = true;
                log.debug("Factor value: " + factorValue + " not present in efvsToPlot (" + StringUtils.join(efvsToPlot, ",") + "), " +
                        "flagging this series insignificant");
            }

            seriesList.add(series);
        }

        // create the mean series
        seriesList.add(makeMap(
                "data", meanSeriesData,
                "lines", makeMap("show", true, "lineWidth", 1.0, "fill", false),
                "bars", makeMap("show", false),
                "points", makeMap("show", false),
                "color", "#5e5e5e",
                "label", "Mean",
                "legend", makeMap("show", false),
                "hoverable", "false",
                "shadowSize", 2));

        // and put all data into the plot, flagging whether it is significant or not
        return makeMap(
                "series", seriesList,
                "options", makeMap(
                        "xaxis", makeMap("ticks", 0),
                        "legend", makeMap("show", true,
                                "position", "sw",
                                "insigLegend", makeMap("show", insignificantSeries),
                                "noColumns", 1),
                        "grid", makeMap(
                                "backgroundColor", "#fafafa",
                                "autoHighlight", false,
                                "hoverable", true,
                                "clickable", true,
                                "borderWidth", 1),
                        "arrayDesign", arrayDesignDescription
                ));
    }


    private Map<String, Object> createThumbnailPlot(String ef, String efv, ExpressionAnalysis ea)
            throws IOException {
        log.debug("Creating thumbnail plot... EF: {}, Top FVs: {}, ExpressionAnalysis: {}",
                new Object[]{ef, efv, ea});

        List<Object> seriesData = new ArrayList<Object>();
        int startMark = 0;
        int endMark = 0;
         // Get assayFVs from the proxy from which ea came
        List<String> assayFVs = atlasNetCDFDAO.getFactorValues(ea.getProxyId(), ef);
        List<String> uniqueFVs = sortUniqueFVs(assayFVs);
        // Get actual expression data from the design element stored in ea
        List<Float> expressions = atlasNetCDFDAO.getExpressionData(ea.getProxyId(), ea.getDesignElementIndex());


        // iterate over each factor value (in sorted order)
        for (String factorValue : uniqueFVs) {
            // mark start position, in list of all samples, of the factor value we're after
            if (factorValue.equals(efv)) {
                startMark = seriesData.size() + 1;
            }

            for (int assayIndex = 0; assayIndex < assayFVs.size(); assayIndex++)
                if (assayFVs.get(assayIndex).equals(factorValue)) {
                    float value = expressions.get(assayIndex);
                    seriesData.add(Arrays.<Number>asList(seriesData.size() + 1, value <= -1000000 ? null : value));
                }

            // mark end position, in list of all samples, of the factor value we're after
            if (factorValue.equals(efv)) {
                endMark = seriesData.size();
            }
        }

        return makeMap(
                "series", Collections.singletonList(makeMap(
                        "data", seriesData,
                        "lines", makeMap("show", true, "lineWidth", 2, "fill", false),
                        "legend", makeMap("show", false))),
                "options", makeMap(
                        "xaxis", makeMap("ticks", 0),
                        "yaxis", makeMap("ticks", 0),
                        "legend", makeMap("show", false),
                        "colors", Collections.singletonList("#edc240"),
                        "grid", makeMap(
                                "backgroundColor", "#f0ffff",
                                "autoHighlight", false,
                                "hoverable", true,
                                "clickable", true,
                                "borderWidth", 1,
                                "markings", Collections.singletonList(
                                        makeMap("xaxis", makeMap("from", startMark, "to", endMark),
                                                "color", "#F5F5DC"))
                        ),
                        "selection", makeMap("mode", "x")
                )
        );
    }

    /**
     *
     * @param ef experimental factor for which all efvs are to be plotted
     * @param genes for which plots are to be
     * Note that ea contains proxyId and designElement index from which it came, so that
     * the actual expression values can be easily retrieved later.
     *
     * @return
     * @throws IOException
     */
    private Map<String,Object> createLargePlot(final NetCDFProxy netCDF,
                                               final String ef,
                                               final List<AtlasGene> genes,
                                               final String experimentID)
            throws IOException {

        log.debug("Creating big plot... EF: {}, Gene Names: [{}]",
                  new Object[]{ef, StringUtils.join(genes, " ")});

        // data for individual series
        List<Object> seriesList = new ArrayList<Object>();

        // Arrays.asList() returns an unmodifiable list - we need to wrap it into a modifiable
        // list to be able to remove EMTPY_EFV from it.
        final List<String> assayFVs = new ArrayList(Arrays.asList(netCDF.getFactorValues(ef)));

        List<String> uniqueFVs = sortUniqueFVs(assayFVs);

        // Don't plot (empty) efvs
        if (uniqueFVs.contains(EMPTY_EFV)) {
            uniqueFVs.remove(EMPTY_EFV);
        }

        // Map: geneId -> bestEA (the one fwith the lowest pValue) EA across all efvs for this ef
        final Map<String, ExpressionAnalysis> geneIdToBestEAAcrossAllEfvs =
                new HashMap<String, ExpressionAnalysis>();

        // iterate over design elements axis
        // fixme: this assumes gene indices and design element indices are the same, actually we need to do a lookup
        for (AtlasGene gene : genes) {

            // Find ExpressionAnalysis with the lowest pValue across all efvs
            Long geneId = Long.parseLong(gene.getGeneId());

            // Find best pValue expressions for geneId and ef in netCDF.getId() - it's expression values for these
            // that will be plotted
            Map<String, ExpressionAnalysis> bestEAsPerEfvInProxy =
                    atlasNetCDFDAO.getBestEAsPerEfvInProxy(netCDF.getId(), geneId, ef);

            ExpressionAnalysis bestEAAcrossAllEfvs = null;
            for (ExpressionAnalysis ea : bestEAsPerEfvInProxy.values()) {
                if (bestEAAcrossAllEfvs == null ||
                        // lower pVals, or for the same pVal, higher tStats are better
                        bestEAAcrossAllEfvs.getPValAdjusted() > ea.getPValAdjusted() ||
                        (bestEAAcrossAllEfvs.getPValAdjusted()== ea.getPValAdjusted() && bestEAAcrossAllEfvs.getTStatistic() < ea.getTStatistic())) {
                    bestEAAcrossAllEfvs = ea;
                }
            }
            geneIdToBestEAAcrossAllEfvs.put(gene.getGeneId(), bestEAAcrossAllEfvs);

            // Get expression data corresponding to the min pValue across all efvs
            List<Float> expressions =
                    atlasNetCDFDAO.getExpressionData(
                            bestEAAcrossAllEfvs.getProxyId(), bestEAAcrossAllEfvs.getDesignElementIndex());

            // create series objects for this row of the data matrix
            List<List<Number>> seriesData = new ArrayList<List<Number>>();

            // Populate seriesData so that each unique fv's expression data is plotted together
            for (String factorValue : uniqueFVs) {
                for (int assayIndex = 0; assayIndex < assayFVs.size(); assayIndex++)
                    if (assayFVs.get(assayIndex).equals(factorValue)) {
                        float value = expressions.get(assayIndex);
                        seriesData.add(Arrays.<Number>asList(0.5d + seriesData.size(), value <= -1000000 ? null : value));
                    }
            }

            Map<String,Object> series = makeMap(
                    "data", seriesData,
                    // store some standard config info
                    "lines", makeMap("show", "true", "lineWidth", 2, "fill", false, "steps", false),
                    "points", makeMap("show", true, "fill", true),
                    "legend", makeMap("show", true),
                    "label", makeMap("id", gene.getGeneId(), "identifier", gene.getGeneIdentifier(), "name", gene.getGeneName()));

            // store the plot order for this gene
            series.put("color", seriesList.size());

            // and save this series
            seriesList.add(series);
        }

        // Populate sortedAssayOrder - list of assay indexes for each plotted at
        List<Integer> sortedAssayOrder = populateSortedAssayOrder(uniqueFVs, assayFVs);

        final Map<String, List<Long>> proxyIdToDesignElements =
                atlasNetCDFDAO.getProxyIdToDesignElements(experimentID);
        final Map<String, Long> proxyIdToArrayDesignId =
                atlasNetCDFDAO.getProxyIdToArrayDesignId(experimentID);

        final List<String> efs = Arrays.asList(netCDF.getFactors());
        final List<String> scs = Arrays.asList(netCDF.getCharacteristics());
        final int[][] bs2as = netCDF.getSamplesToAssays();

        final Map<String, List<String>> scvs = new HashMap<String, List<String>>();
        for(String i : scs)
            scvs.put(i, Arrays.asList(netCDF.getCharacteristicValues(i)));

        // Restrict the plotted data count in seriesList and sortedAssayOrder to max. MAX_DATAPOINTS_PER_ASSAY
        populationControl(seriesList, assayFVs, uniqueFVs, sortedAssayOrder);

        // Get plot marking-per-efv data
        List<Map> markings = getMarkings(sortedAssayOrder, assayFVs);

        return makeMap(
                "ef", ef,
                "series", seriesList,
                "simInfo", new FilterIterator<AtlasGene,Map>(genes.iterator()) {
                    public Map map(AtlasGene atlasGene) {
                        ExpressionAnalysis ea =
                                geneIdToBestEAAcrossAllEfvs.get(atlasGene.getGeneId());

                        // get array design id
                        Long adID = null;
                        try {
                           adID = proxyIdToArrayDesignId.get(ea.getProxyId());
                            if (adID == -1) {
                                String adAcc = atlasNetCDFDAO.getArrayDesignAccession(ea.getProxyId());
                                adID = getAtlasDatabaseDAO().getArrayDesignByAccession(adAcc).getArrayDesignID();
                            }
                        } catch (IOException ioe) {
                          String errMsg = "Failed to find array design id or accession in proxy id: " + ea.getProxyId();
                            log.error(errMsg);
                            throw new RuntimeException(errMsg);
                        }

                        return ea != null ? makeMap(
                                "deId", proxyIdToDesignElements.get(ea.getProxyId()).get(ea.getDesignElementIndex()),
                                "adId", adID,
                                "name", atlasGene.getGeneName()
                        ) : null;
                    }
                },
                "assayOrder", sortedAssayOrder.iterator(),
                "assayProperties", new MappingIterator<Integer,Map>(sortedAssayOrder.iterator()) {
                    public Map map(final Integer assayIndex) {
                        return makeMap(
                                "efvs", new FilterIterator<String,Map>(efs.iterator()) {
                                    public Map map(String ef) {
                                        String v = assayFVs.get(assayIndex);
                                        return v.length() > 0 ? makeMap("k", ef, "v", v) : null;
                                    }
                                },
                                "scvs", new FlattenIterator<Integer,Map>(
                                        new FilterIterator<Integer,Integer>(CountIterator.zeroTo(bs2as.length)) {
                                            public Integer map(Integer sampleIndex) {
                                                return bs2as[sampleIndex][assayIndex] == 1 ? sampleIndex : null;
                                            }
                                        }
                                ) {
                                    public Iterator<Map> inner(final Integer sampleIndex) {
                                        return new MappingIterator<String,Map>(scs.iterator()) {
                                            public Map map(String sc) {
                                                String v = scvs.get(sc).get(sampleIndex);
                                                return v.length() > 0 ? makeMap("k", sc, "v", v) : null;
                                            }
                                        };
                                    }
                                }
                        );
                    }
                },
                "options", makeMap(
                        "xaxis", makeMap("ticks", 0),
                        "yaxis", makeMap("ticks", 3),
                        "series", makeMap(
                                "points", makeMap("show", true, "fill", true, "radius", 1.5),
                                "lines", makeMap("show", true, "steps", false)),
                        "legend", makeMap("show", true),
                        "grid", makeMap(
                                "backgroundColor", "#fafafa",
                                "autoHighlight", true,
                                "hoverable", true,
                                "clickable", true,
                                "borderWidth", 0,
                                "markings", markings),
                        "selection", makeMap("mode","x")
                ));
    }


    /**
     *
     * @param uniqueFVs
     * @param assayFVs
     * @return List of assay indexes in the order in which expression data in these assay indexes will be plotted (i.e. by efv)
     */
    private List<Integer> populateSortedAssayOrder(List<String> uniqueFVs, List<String> assayFVs) {
        List<Integer> sortedAssayOrder = new ArrayList<Integer>(assayFVs.size());
        for(String factorValue : uniqueFVs) {
            int assayIndex = 0;
            for(String assayFV : assayFVs) {
                if(assayFV.equals(factorValue)) {
                    sortedAssayOrder.add(assayIndex);
                }
                ++assayIndex;
            }
        }
        return sortedAssayOrder;
    }


    /**
     *
     * @param sortedAssayOrder
     * @param assayFVs
     * @return list of plot partitioning data, one partition per efv
     */
    private List<Map> getMarkings(List<Integer> sortedAssayOrder, List<String> assayFVs) {
        List<Map> markings = new ArrayList<Map>();
        int position = 0;
        int start = 0;
        int flicker = 0;
        String prevFactorValue = null;
        String factorValue = null;
        for (Integer assayIndex : sortedAssayOrder) {
            factorValue = assayFVs.get(assayIndex);
            if (prevFactorValue == null) {
                prevFactorValue = factorValue;
            }
            if (!factorValue.equals(prevFactorValue)) {
                markings.add(makeMap(
                        "xaxis", makeMap("from", start, "to", position),
                        "label", prevFactorValue.length() > 0 ? prevFactorValue : "unannotated",
                        "color", markingColors[++flicker % 2]
                ));
                prevFactorValue = factorValue;
                start = position;
            }
            position++;
        }
        if (prevFactorValue != null) {
            markings.add(makeMap(
                    "xaxis", makeMap("from", start, "to", position),
                    "label", prevFactorValue.length() > 0 ? prevFactorValue : "unannotated",
                    "color", markingColors[++flicker % 2]
            ));
        }
        return markings;
    }

    /**
     *
     * @param seriesList List of expression data series to be used in the plot
     * @param efvs List of efvs for ef in an assay
     * @param sortedAssayOrder List of assay indexes in the order in which expression data will be displayed, sorted by factor values, and by assay index within each factor value
     */
    private void populationControl(List<Object> seriesList,
                                   final List<String> efvs,
                                   final List<String> uniqueFVs,
                                   List<Integer> sortedAssayOrder) {

        int thisAssayCount = efvs.size();

        if (thisAssayCount <= MAX_DATAPOINTS_PER_ASSAY)
            // We are already plotting less than the maximum - no need to reduce the amount of plotted expresison data
            return;

        // Assemble (in survivors) a list of assay indexes that will be retained in the plot, processing each efv in distinctValues
        // until we reach MAX_DATAPOINTS_PER_ASSAY of retained assay indexes
        List<Integer> survivors = new ArrayList<Integer>();
        int target = thisAssayCount < MAX_DATAPOINTS_PER_ASSAY ? thisAssayCount : MAX_DATAPOINTS_PER_ASSAY;
        for (; survivors.size() <= target;) {
            for (String fv : uniqueFVs) {
                for (int assayIndex = 0; (survivors.size() <= target) && (assayIndex < thisAssayCount); assayIndex++) {
                    if (fv.equals(efvs.get(assayIndex))) {
                        if (!survivors.contains(assayIndex)) {
                            survivors.add(assayIndex);
                            break;
                        }
                    }
                }
            }
        }

        // For all series in seriesList, retain expression data corresponding to assay indexes in survivors
        for (Object o : seriesList) {

            List<List<Number>> oldData = (List<List<Number>>) ((HashMap) o).get("data");
            List<List<Number>> data = keepSurvivors(oldData, sortedAssayOrder, survivors, efvs);

            ((HashMap) o).put("data", data);
        }

        // In the sorted List of assay indexes in the order in which expression data will be displayed, retain assay indexes in survivors
        sortedAssayOrder.retainAll(survivors);
    }

    /**
     * @param oldData
     * @param sortedAssayOrder
     * @return retain only data corresponding to assayIndexes in sortedAssayOrder
     */
    private List<List<Number>> keepSurvivors(
            final List<List<Number>> oldData,
            final List<Integer> sortedAssayOrder,
            final List<Integer> survivors,
            List<String> assayFVs) {

        List<List<Number>> result = new ArrayList<List<Number>>();
        for (Integer pos = 0; pos < sortedAssayOrder.size(); pos++) {
            int assayIndex = sortedAssayOrder.get(pos);
            if (survivors.contains(assayIndex)) {
                result.add(Arrays.<Number>asList(0.5d + result.size(), oldData.get(pos).get(1)));
            }
        }
        return result;
    }

    private static List<String> sortUniqueFVs(List<String> assayFVs) {
        HashSet<String> uniqueSet = new HashSet<String>(assayFVs);
        List<String> uniqueFVs = new ArrayList<String>(uniqueSet);
        Collections.sort(uniqueFVs, new Comparator<String>() {
            public int compare(String s1, String s2) {
                // want to make sure that empty strings are pushed to the back
                if (s1.equals("") && s2.equals("")) {
                    return 0;
                }
                if (s1.equals("") && !s2.equals("")) {
                    return 1;
                }
                if (!s1.equals("") && s2.equals("")) {
                    return -1;
                }

                java.util.regex.Matcher m1 = startsOrEndsWithDigits.matcher(s1);
                java.util.regex.Matcher m2 = startsOrEndsWithDigits.matcher(s2);

                if (m1.find() && m2.find()) {
                    Long i1 = new Long(s1.substring(m1.start(), m1.end()));
                    Long i2 = new Long(s2.substring(m2.start(), m2.end()));

                    if (i1.compareTo(i2) == 0) {
                        return s1.compareToIgnoreCase(s2);
                    }
                    else {
                        return i1.compareTo(i2);
                    }
                }

                return s1.compareToIgnoreCase(s2);
            }
        });
        return uniqueFVs;
    }

    /**
     * @param efToEfvToEA
     * @return the ef with the lowest pValue from ExpressionAnalyses in efToEfvToEA
     */
    private String getHighestRankEF(Map<String, Map<String, ExpressionAnalysis>> efToEfvToEA) {
        String bestEf = null;
        Float bestPValue = null;
        Float bestTStat = null;
        for (String ef : efToEfvToEA.keySet()) {
            for (ExpressionAnalysis ea : efToEfvToEA.get(ef).values()) {
                // lower pVals, or for the same pVals, higher tStats, are better
                if (bestPValue == null || bestPValue > ea.getPValAdjusted()
                        || (bestPValue == ea.getPValAdjusted() && bestTStat < ea.getTStatistic())) {
                    bestEf = ea.getEfName();
                    bestPValue = ea.getPValAdjusted();
                    bestTStat = ea.getTStatistic();
                }
            }
        }
        return bestEf;
    }
}

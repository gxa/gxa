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
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

import uk.ac.ebi.gxa.requesthandlers.api.result.ExperimentResultAdapter;
import uk.ac.ebi.gxa.utils.*;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.File;
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
                                    final String plotType,
                                    String de) {

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
            // the actual expression values can be easily retrieved later
            proxy = atlasNetCDFDAO.getNetCDFProxy(atlasNetCDFDAO.findProxyId(experimentID, null));
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    atlasNetCDFDAO.getExpressionAnalysesForGeneIds(geneIds, experimentID, proxy);

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
            } else if (plotType.equals("bar")) {
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
            if (proxy != null) {
                proxy.close();
            }
        }
        return null;
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

        String bestProxyId = null;
        if (efvClickedOn != null && efvToBestEA.get(efvClickedOn) != null) {
            // If the user has clicked on an efv, choose to plot expression data from NetCDF proxy in which
            // the best pValue for this proxy occurred.
            bestProxyId = efvToBestEA.get(efvClickedOn).getProxyId();
        } else { // The user hasn't clicked on an efv - choose the proxy in most besEA across all efvs
            bestProxyId = getBestProxyId(efvToBestEA.values());
        }

        NetCDFProxy proxy = atlasNetCDFDAO.getNetCDFProxy(bestProxyId);

        // Get unique factors from proxy
        List<String> assayFVs = new ArrayList<String>(Arrays.asList(proxy.getFactorValues(ef)));

        // Find array design accession for bestProxyId - this will be displayed under the plot
        String arrayDesignAcc = null;
        try {
            arrayDesignAcc = proxy.getArrayDesignAccession();
        } finally {
            proxy.close();
        }
        String arrayDesignName = atlasDatabaseDAO.getArrayDesignShallowByAccession(arrayDesignAcc).getName();
        String arrayDesignDescription = arrayDesignAcc + (arrayDesignName != null ? " " + arrayDesignName : "");

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
        NetCDFProxy proxy = atlasNetCDFDAO.getNetCDFProxy(ea.getProxyId());
        List<String> assayFVs = new ArrayList<String>();
        try {
            assayFVs.addAll(Arrays.asList(proxy.getFactorValues(ef)));
        } finally {
            proxy.close();
        }
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
     * Method collecting plot data for the experiment line plot
     *
     * @param netCDF proxy from which plotted data is obtained
     * @param ef   experimental factor being plotted
     * @param bestDEIndexToGene Map designIndex with the best expression stats -> AtlasGene, containing top genes for the experiment being plotted
     * @param deIndexToBestExpressions Map designIndex -> expression data ( bestDEIndexToGene.keySet() is the same asdeIndexToBestExpressions.keySet())
     * @param assayFVs  List of all efvs for ef in an assay
     * @param uniqueFVs List of unique FVs in an assay (excluding EMPTY_EFV)
     * @param scs List of sample characteristics retrieved from netCDF
     * @param bs2as sample to assay mapping array, retrieved from netCDF
     * @param scvs List of sample characteristic valuesm retrievedfrom netCDF
     * @return Map containing plot data that will be delivered to javascript via JSON
     * @throws IOException
     */
    public Map<String,Object> createLargePlot(final NetCDFProxy netCDF,
                                               final String ef,
                                               final Map<String, AtlasGene> bestDEIndexToGene,
                                               final Map<String, List<Float>> deIndexToBestExpressions,
                                               final List<String> assayFVs,
                                               final List<String> uniqueFVs,
                                               final List<String> scs,
                                               final int[][] bs2as,
                                               final Map<String, List<String>> scvs
                                               )
            throws IOException {

        log.debug("Creating big plot... EF: {}, Design Element Indexes: [{}]",
                  new Object[]{ef, StringUtils.join(deIndexToBestExpressions.keySet(), " ")});

        // data for individual series
        List<Object> seriesList = new ArrayList<Object>();

        for (String deIndex : deIndexToBestExpressions.keySet()) {

            AtlasGene gene = bestDEIndexToGene.get(deIndex);
            // create series objects for this row of the data matrix
            List<List<Number>> seriesData = new ArrayList<List<Number>>();

            List<Float> expressionsForDE = deIndexToBestExpressions.get(deIndex);
            for (String factorValue : uniqueFVs) {
                for (int assayIndex = 0; assayIndex < assayFVs.size(); assayIndex++) {
                    if (assayFVs.get(assayIndex).equals(factorValue)) {
                        seriesData.add(Arrays.<Number>asList(0.5d + seriesData.size(), expressionsForDE.get(assayIndex) <= -1000000 ? null : expressionsForDE.get(assayIndex)));
                    }
                }
            }

            Map<String,Object> series = makeMap(
                    "data", seriesData,
                    // store some standard config info
                    "lines", makeMap("show", "true", "lineWidth", 2, "fill", false, "steps", false),
                    "points", makeMap("show", true, "fill", true),
                    "legend", makeMap("show", true),
                    "label", makeMap( "id", netCDF.getDesignElements()[Integer.valueOf(deIndex)],
                                      "identifier", gene.getGeneIdentifier(),
                                      "name", gene.getGeneName(),
                                      "designelement",netCDF.getDesignElements()[Integer.valueOf(deIndex)])); //??not called

            // store the plot order for this gene
            series.put("color", seriesList.size());

            // and save this series
            seriesList.add(series);
        }
        // Populate sortedAssayOrder - list of assay indexes for each plotted at
        List<Integer> sortedAssayOrder = populateSortedAssayOrder(uniqueFVs, assayFVs);

        // Restrict the plotted data count in seriesList and sortedAssayOrder to max. MAX_DATAPOINTS_PER_ASSAY
        populationControl(seriesList, assayFVs, uniqueFVs, sortedAssayOrder);

        // Get plot marking-per-efv data
        List<Map> markings = getMarkings(sortedAssayOrder, assayFVs);
        return makeMap(
                "ef", ef,
                "series", seriesList,
                "simInfo", new FilterIterator<String,Map>(deIndexToBestExpressions.keySet().iterator()) {
                    public Map map(String deIndex) {
                        // get array design id
                        Long adID = null;
                        try {
                           adID = netCDF.getArrayDesignID();
                            if (adID == -1) {
                                String adAcc = netCDF.getArrayDesignAccession();
                                adID = getAtlasDatabaseDAO().getArrayDesignShallowByAccession(adAcc).getArrayDesignID();
                            }
                        } catch (IOException ioe) {
                          String errMsg = "Failed to find array design id or accession in proxy id: " + netCDF.getId();
                            log.error(errMsg);
                            throw new RuntimeException(errMsg);
                        }

                        return makeMap(
                                "deId", deIndex,
                                "adId", adID,
                                "name", bestDEIndexToGene.get(deIndex).getGeneName()
                        );
                    }
                },
                "assayOrder", sortedAssayOrder.iterator(),
                "assayProperties", new MappingIterator<Integer, Map>(sortedAssayOrder.iterator()) {
                    public Map map(final Integer assayIndex) {
                        return makeMap(
                                "efvs", (assayFVs.get(assayIndex).length() > 0 ? makeMap("k", ef, "v", assayFVs.get(assayIndex)) : null),
                                "scvs", new FlattenIterator<Integer, Map>(
                                        new FilterIterator<Integer, Integer>(CountIterator.zeroTo(bs2as.length)) {
                                            public Integer map(Integer sampleIndex) {
                                                return bs2as[sampleIndex][assayIndex] == 1 ? sampleIndex : null;
                                            }
                                        }
                                ) {
                                    public Iterator<Map> inner(final Integer sampleIndex) {
                                        return new MappingIterator<String, Map>(scs.iterator()) {
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
     * @param seriesList       List of expression data series to be used in the plot
     * @param efvs             List of all efvs for ef in an assay
     * @param uniqueFVs        List of unique FVs in an assay (excluding EMPTY_EFV)
     * @param sortedAssayOrder List of assay indexes in the order in which expression data will be displayed, sorted by factor values, and by assay index within each factor value
     */
    private void populationControl(List<Object> seriesList,
                                   final List<String> efvs,
                                   final List<String> uniqueFVs,
                                   List<Integer> sortedAssayOrder) {

        int assayCount = efvs.size();
        // Note that thisAssayCount excludes EMPTY_EFVS as these will have been excluded from uniqueFVs (and thus are not plotted)
        int plottableAssayCount = assayCount - Collections.frequency(efvs, EMPTY_EFV);


        if (plottableAssayCount <= MAX_DATAPOINTS_PER_ASSAY)
            // We are already plotting less than the maximum - no need to reduce the amount of plotted expression data
            return;

        // Assemble (in survivors) a list of assay indexes that will be retained in the plot, processing each efv in distinctValues
        // until we reach MAX_DATAPOINTS_PER_ASSAY of retained assay indexes
        List<Integer> survivors = new ArrayList<Integer>();
        int target = plottableAssayCount < MAX_DATAPOINTS_PER_ASSAY ? plottableAssayCount : MAX_DATAPOINTS_PER_ASSAY;

        int survivorsCnt = 0;
        while (survivorsCnt <= target) {
            for (String fv : uniqueFVs) {
                for (int assayIndex = 0; survivorsCnt <= target && assayIndex < assayCount; assayIndex++) {
                    if (fv.equals(efvs.get(assayIndex))) {
                        if (!survivors.contains(assayIndex)) {
                            survivors.add(assayIndex);
                            survivorsCnt++;
                            break;
                        }
                    }
                }
            }
        }

        // For all series in seriesList, retain expression data corresponding to assay indexes in survivors
        for (Object o : seriesList) {

            List<List<Number>> oldData = (List<List<Number>>) ((HashMap) o).get("data");
            List<List<Number>> data = keepSurvivors(oldData, sortedAssayOrder, survivors);

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
            final List<Integer> survivors) {

        List<List<Number>> result = new ArrayList<List<Number>>();
        for (Integer pos = 0; pos < sortedAssayOrder.size(); pos++) {
            int assayIndex = sortedAssayOrder.get(pos);
            if (survivors.contains(assayIndex)) {
                result.add(Arrays.<Number>asList(0.5d + result.size(), oldData.get(pos).get(1)));
            }
        }
        return result;
    }

    /**
     * Method collecting plot data for the experiment box plot
     *
     * @param netCDF proxy from which plotted data is obtained
     * @param ef experimental factor being plotted
     * @param bestDEIndexToGene Map designIndex with the best expression stats -> AtlasGene, containing top genes for the experiment being plotted
     * @param deIndexToBestExpressions  Map designIndex -> expression data ( bestDEIndexToGene.keySet() is the same asdeIndexToBestExpressions.keySet())
     * @param assayFVs             List of all efvs for ef in an assay
     * @param uniqueFVs        List of unique FVs in an assay (excluding EMPTY_EFV)
     * @return Map containing plot data that will be delivered to javascript via JSON
     * @throws IOException
     */
    public Map<String, Object> createBoxPlot(final NetCDFProxy netCDF,
                                             final String ef,
                                             final Map<String, AtlasGene> bestDEIndexToGene,
                                             final Map<String, List<Float>> deIndexToBestExpressions,
                                             final List<String> assayFVs,
                                             final List<String> uniqueFVs)

            throws IOException {
         log.debug("Creating big plot... EF: {}, Design Element Indexes: [{}]",
                  new Object[]{ef, StringUtils.join(deIndexToBestExpressions.keySet(), " ")});

        BoxPlot boxPlot = new BoxPlot();
        boxPlot.series = new ArrayList<DataSeries>();

        boxPlot.factorValues = uniqueFVs;
        boxPlot.numDesignElements = deIndexToBestExpressions.keySet().size();
        int iGene = 0; //ordinal number of gene - to make color from it
        for (String deIndex : deIndexToBestExpressions.keySet()) {

            AtlasGene gene = bestDEIndexToGene.get(deIndex);
            DataSeries dataSeries = new DataSeries();
            boxPlot.series.add(dataSeries);

            dataSeries.gene = gene;
            dataSeries.data = new ArrayList<BoxAndWhisker>();
            dataSeries.color = String.format("%d", iGene);

            dataSeries.designelement = String.valueOf(netCDF.getDesignElementId(Integer.parseInt(deIndex)));

            int iFactorValue = 0;
            List<Float> expressionsForDE = deIndexToBestExpressions.get(deIndex);

            for (String factorValue : uniqueFVs) {
                List<Float> values = new ArrayList<Float>();
                for (int assayIndex = 0; assayIndex < assayFVs.size(); assayIndex++) {
                    if (assayFVs.get(assayIndex).equals(factorValue)) {
                            values.add(expressionsForDE.get(assayIndex));
                    }

                }
                dataSeries.data.add(new BoxAndWhisker(gene.getGeneName() + ":" + factorValue, values, iFactorValue * boxPlot.numDesignElements + iGene));
                iFactorValue++;
            }
            iGene++;
        }

        Map<String, Object> o = boxPlot.toMap();

        return o;
    }

     public class BoxPlot{
         public List<DataSeries> series;
         public Integer maxValue;
         public Integer minValue;

         public List<String> factorValues;
         public int numDesignElements;

         public Map<String,Object> toMap(){
             List<Object> serialized_series = new ArrayList<Object>();
             for(DataSeries dataSeries : series){
                 serialized_series.add(dataSeries.toMap());
             }

             List<Map> markings = new ArrayList<Map>();
             int start = 0;
             int flicker = 0;
             for(String factorValue : factorValues) {
                 markings.add(makeMap(
                         "xaxis", makeMap("from", start, "to", start+numDesignElements),
                         "label", factorValue.length() > 0 ? factorValue : "unannotated",
                         "color", markingColors[++flicker % 2]
                         ));
                 start = start+numDesignElements;
             }
             return makeMap("minValue",minValue,
                            "maxValue",maxValue,
                            "series", serialized_series,
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
                                "selection", makeMap("mode","x")));
         }
     }



     public class DataSeries{
         public AtlasGene gene;
         public String color;
         public List<BoxAndWhisker> data;
         public String designelement;
         public Map<String,Object> toMap(){
             List<Object> serialized_data = new ArrayList<Object>();
             for(BoxAndWhisker boxAndWhisker : data){
                 serialized_data.add(boxAndWhisker.toMap());
             }
             return makeMap(
                     "label", makeMap("id", gene.getGeneId(), "identifier", gene.getGeneIdentifier(), "name", gene.getGeneName(), "designelement", designelement),
                     "color", color,
                     "data", serialized_data);
         }
     }

     public class BoxAndWhisker{
         public String id; //        ="megatron" + i;
         public double median; //=3.9;
         public double uq; //=4.3;
         public double lq; //=3.1;
         public double max; //=5.2;
         public double min; //=1.7;
         public int x; //ordinal number of the box;

         public BoxAndWhisker(){
             id ="name";
             median =3.9;
             uq=4.3;
             lq=3.1;
             max=5.2;
             min=1.7;
         }

         public BoxAndWhisker(String id){
             this();
             this.id=id;
         }

         public BoxAndWhisker(String id, List<Float> data, int x){
             this();
             this.id=id;
             Collections.sort(data);
             this.median=data.get(data.size()/2);
             this.max=data.get(data.size()-1);
             this.min=data.get(0);
             this.uq=data.get(data.size()*3/4);
             this.lq=data.get(data.size()*1/4);
             this.x=x;
         }

         public Map<String,Object> toMap(){
             return makeMap("id",id ,
                             "median",median,
                             "uq",uq,
                             "lq",lq,
                             "max",max,
                             "min",min,
                             "x",x   );
         }
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


    public Map<String, Map<String, Map<String, Object>>> getExperimentPlots(
            NetCDFProxy proxy,
            ExperimentResultAdapter.ArrayDesignExpression.DesignElementExpMap designElementExpressions,
            Collection<AtlasGene> genes,
            Collection<String> designElementIndexes) {
        Map<String, Map<String, Map<String, Object>>> efToPlotTypeToData = new HashMap<String, Map<String, Map<String, Object>>>();

        String adAccession = null;
        try {
            long start = System.currentTimeMillis();
            adAccession = proxy.getArrayDesignAccession();
            Map<String, List<Float>> deIndexToExpressions = new HashMap<String, List<Float>>();
            // We used LinkedHashMap() because we need to preserve the order of deIndex keys in the map
            Map<String, AtlasGene> bestDEIndexToGene = new LinkedHashMap<String, AtlasGene>();
            Iterator<String> deIndexesIterator = designElementIndexes.iterator();
            // NB. designElementIds[i] corresponds to a design element in which best expression analytic exists for gene[i]
            for (AtlasGene gene : genes) {
                String deIndex = deIndexesIterator.next();
                deIndexToExpressions.put(deIndex, IteratorUtils.toList(designElementExpressions.get(deIndex).iterator()));
                bestDEIndexToGene.put(deIndex, gene);
            }

            final List<String> efs = Arrays.asList(proxy.getFactors());
            final List<String> scs = Arrays.asList(proxy.getCharacteristics());
            final int[][] bs2as = proxy.getSamplesToAssays();

            final Map<String, List<String>> efvs = new HashMap<String, List<String>>();
            for (String ef : efs)
                efvs.put(ef, Arrays.asList(proxy.getFactorValues(ef)));

            final Map<String, List<String>> scvs = new HashMap<String, List<String>>();
            for (String i : scs)
                scvs.put(i, Arrays.asList(proxy.getCharacteristicValues(i)));


            for (String ef : efs) {
                // Arrays.asList() returns an unmodifiable list - we need to wrap it into a modifiable
                // list to be able to remove EMTPY_EFV from it.
                List<String> assayFVs = efvs.get(ef);
                List<String> uniqueFVs = sortUniqueFVs(assayFVs);
                // Don't plot (empty) efvs
                if (uniqueFVs.contains(EMPTY_EFV)) {
                    uniqueFVs.remove(EMPTY_EFV);
                }

                Map<String, Map<String, Object>> plotTypeToData = makeMap(
                        "large", createLargePlot(proxy, ef, bestDEIndexToGene, deIndexToExpressions, assayFVs, uniqueFVs, scs, bs2as, scvs),
                        "box", createBoxPlot(proxy, ef, bestDEIndexToGene, deIndexToExpressions, assayFVs, uniqueFVs)
                );
                efToPlotTypeToData.put(ef, plotTypeToData);
            }
            log.info("getExperimentPlots() for DEs: ("+ bestDEIndexToGene.keySet() + ") took " + (System.currentTimeMillis() - start) + " ms");
        } catch (IOException ioe) {
            log.error("Failed to generate plot data for array design: " + adAccession, ioe);
        } finally {
            if (proxy != null) {
                proxy.close();
            }
        }
        return efToPlotTypeToData;
    }
}

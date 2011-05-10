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

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.requesthandlers.api.result.ExperimentResultAdapter;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;
import static uk.ac.ebi.gxa.netcdf.reader.NetCDFPredicates.containsEfEfv;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

public class AtlasPlotter {
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private AtlasDAO atlasDatabaseDAO;
    private GeneSolrDAO geneSolrDAO;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String[] altColors = {"#D8D8D8", "#E8E8E8"};
    private static final String[] markingColors = {"#F0FFFF", "#F5F5DC"};
    private static final Pattern startsOrEndsWithDigits = java.util.regex.Pattern.compile("^\\d+|\\d+$");
    // This constant is used to prevent empty efvs from being displayed in plots (cf. SDRFWritingUtils)
    private static final String EMPTY_EFV = "(empty)";

    private static final int MAX_POINTS_IN_THUMBNAIL = 500;

    public void setAtlasDatabaseDAO(AtlasDAO atlasDatabaseDAO) {
        this.atlasDatabaseDAO = atlasDatabaseDAO;
    }

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }


    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public Map<String, Object> getGeneInExpPlotData(final String geneIdKey,
                                                    final String experimentAccession,
                                                    final String ef,
                                                    final String efv,
                                                    final String plotType) {

        log.debug("Plotting gene {}, experiment {}, factor {}", new Object[]{geneIdKey, experimentAccession, ef});
        try {
            final List<AtlasGene> genes = parseGenes(geneIdKey);

            // geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
            // Note that ea contains proxyId and designElement index from which it came, so that
            // the actual expression values can be easily retrieved later
            final Collection<Long> geneIds = transform(genes, new Function<AtlasGene, Long>() {
                public Long apply(@Nonnull AtlasGene input) {
                    return (long) input.getGeneId();
                }
            });
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    atlasNetCDFDAO.getExpressionAnalysesForGeneIds(experimentAccession, geneIds, containsEfEfv(ef, efv));
            if (geneIdsToEfToEfvToEA == null)
                return null;

            String efToPlot;

            if ("default".equals(ef)) {
                Long geneId = (long) genes.get(0).getGeneId();
                // First try to get the highest ranking from top gene
                efToPlot = getHighestRankEF(geneIdsToEfToEfvToEA.get(geneId));
            } else {
                efToPlot = ef;
            }

            if (efToPlot == null)
                throw logUnexpected("Can't find EF to plot");

            if (plotType.equals("thumb")) {
                AtlasGene geneToPlot = genes.get(0);
                Long geneId = (long) geneToPlot.getGeneId();
                final Map<String, Map<String, ExpressionAnalysis>> geneDetails = geneIdsToEfToEfvToEA.get(geneId);
                if (geneDetails == null)
                    throw logUnexpected("Can't find analysis data for gene " + geneId);
                final Map<String, ExpressionAnalysis> analysisForEF = geneDetails.get(efToPlot);
                if (analysisForEF == null)
                    throw logUnexpected("Can't find analysis data for gene " + geneId + ", " +
                            " EF '" + efToPlot + "'");
                ExpressionAnalysis bestEA = analysisForEF.get(efv);
                if (bestEA == null)
                    throw logUnexpected("Can't find deIndex for min pValue for gene " + geneId + ", " +
                            " EF '" + efToPlot + "', value '" + efv + "'");
                return createThumbnailPlot(efToPlot, efv, bestEA, experimentAccession);
            } else if (plotType.equals("bar")) {
                AtlasGene geneToPlot = genes.get(0);
                Long geneId = (long) geneToPlot.getGeneId();
                Map<String, ExpressionAnalysis> efvToBestEA = geneIdsToEfToEfvToEA.get(geneId).get(efToPlot);
                if (!efvToBestEA.isEmpty())
                    return createBarPlot(geneId, efToPlot, efv, efvToBestEA, experimentAccession);
            }

        } catch (IOException e) {
            throw logUnexpected("IOException whilst trying to read from NetCDFs for experiment " + experimentAccession, e);
        }
        return null;
    }

    private List<AtlasGene> parseGenes(String geneIdKey) {
        List<AtlasGene> genes = new ArrayList<AtlasGene>();

        // lookup gene names, again using SOLR index
        for (String geneIdStr : geneIdKey.split(",")) {
            GeneSolrDAO.AtlasGeneResult gene = geneSolrDAO.getGeneById(Long.parseLong(geneIdStr));
            if (gene.isFound()) {
                AtlasGene atlasGene = gene.getGene();
                genes.add(atlasGene);
            }
        }
        if (genes.isEmpty()) {
            throw logUnexpected("No existing genes specified by query: geneIdKey = '" + geneIdKey + "'");
        }

        return genes;
    }

    /**
     * @param eas
     * @return find proxy id which occurs most often in ExpressionAnalyses in eas
     */
    private String getBestProxyId(Collection<ExpressionAnalysis> eas) {
        return getMostFrequentProxyId(getProxyIds(eas));
    }

    /**
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

    private static class AssayInfo {
        private int assayIndex;
        private Float expression;

        private AssayInfo(int assayIndex) {
            this.assayIndex = assayIndex;
        }

        public void setExpression(List<Float> expressions) {
            if (expressions.size() <= assayIndex) {
                throw new IllegalStateException("No expression for assayIndex: " + assayIndex);
            }
            expression = expressions.get(assayIndex);
        }
    }

    private static class FactorValueInfo {
        private String name;
        private Boolean isUpOrDown;
        private Float pValue;
        private List<AssayInfo> assays = new ArrayList<AssayInfo>();
        private boolean isInsignificant;

        public FactorValueInfo(String name) {
            this.name = name;
        }

        public void addAssayIndex(int assayIndex) {
            assays.add(new AssayInfo(assayIndex));
        }

        public void setExpressions(List<Float> expressions) {
            for (AssayInfo assayInfo : assays) {
                assayInfo.setExpression(expressions);
            }
            Collections.sort(assays, new Comparator<AssayInfo>() {
                public int compare(AssayInfo o1, AssayInfo o2) {
                    return -1 * compareExpressions(o1, o2);
                }

                private int compareExpressions(AssayInfo o1, AssayInfo o2) {
                    Float e1 = o1.expression;
                    Float e2 = o2.expression;
                    if (e1 == null && e2 == null) {
                        return 0;
                    } else if (e2 == null) {
                        return 1;
                    } else if (e1 == null) {
                        return -1;
                    }
                    return e1.compareTo(e2);
                }
            });
        }

        public void setPValue(float pValue) {
            this.pValue = pValue;
        }

        public void setUpDown(Boolean upDown) {
            this.isUpOrDown = upDown;
        }

        public void setInsignificant(boolean insignificant) {
            isInsignificant = insignificant;
        }

        public boolean isUp() {
            return this.isUpOrDown != null && this.isUpOrDown;
        }

        public boolean isDown() {
            return this.isUpOrDown != null && !this.isUpOrDown;
        }

        public boolean isUpOrDown() {
            return isUp() || isDown();
        }

        public Float maxValue() {
            return assays.get(0).expression;
        }

        public boolean isEmpty() {
            return assays.isEmpty();
        }

        public void reduce(double reduceFactor) {
            int n = (int) Math.floor(reduceFactor * assays.size());
            if (n >= assays.size()) {
                return;
            }

            List<AssayInfo> list = new ArrayList<AssayInfo>();

            if (n > 0 && !assays.isEmpty()) {
                float max = assays.get(0).expression;
                float min = assays.get(assays.size() - 1).expression;
                float dv = Math.abs((max - min) / n);

                float v = max;
                float prevDx = 0;
                AssayInfo prev = null;

                for (AssayInfo assay : assays) {
                    float x = assay.expression;
                    float dx = Math.abs(x - v);
                    if (prev != null && dx > prevDx) {
                        list.add(prev);
                        v -= dv;
                    }
                    prevDx = dx;
                    prev = assay;
                }
            }

            assays.clear();
            assays.addAll(list);
        }

        public int size() {
            return assays.size();
        }
    }

    private static class BarPlotDataBuilder {
        Map<String, FactorValueInfo> fvMap = new HashMap<String, FactorValueInfo>();
        private int numberOfValues = 0;

        public BarPlotDataBuilder(String[] allFactorValues) {
            for (int i = 0; i < allFactorValues.length; i++) {
                String factorValue = allFactorValues[i];
                if (factorValue.equals(EMPTY_EFV)) {
                    continue;
                }
                addFactorValue(factorValue, i);
            }
        }

        private void addFactorValue(String fv, int assayIndex) {
            FactorValueInfo fvInfo = fvMap.get(fv);
            if (fvInfo == null) {
                fvInfo = new FactorValueInfo(fv);
                fvMap.put(fv, fvInfo);
            }
            fvInfo.addAssayIndex(assayIndex);
            numberOfValues++;
        }

        private FactorValueInfo getFvInfo(String fv) {
            FactorValueInfo fvInfo = fvMap.get(fv);
            if (fvInfo == null) {
                throw new IllegalStateException("Factor value: " + fv + " not found in the BarPlotData");
            }
            return fvInfo;
        }

        public List<String> getUniqueFactorValues() {
            List<String> uniqValues = new ArrayList<String>();
            uniqValues.addAll(fvMap.keySet());
            return uniqValues;
        }

        public void removeFactorValue(String fv) {
            fvMap.remove(fv);
        }

        public void setExpressions(String fv, List<Float> expressions) {
            getFvInfo(fv).setExpressions(expressions);
        }

        public void setPValue(String fv, float pValAdjusted) {
            getFvInfo(fv).setPValue(pValAdjusted);
        }

        public void setUpDown(String fv, Boolean upDown) {
            getFvInfo(fv).setUpDown(upDown);
        }

        public void setInsignificant(String fv, boolean b) {
            getFvInfo(fv).setInsignificant(b);
        }

        public Map<String, Object> toSeries(Map<String, Object> addToOptions) {
            List<FactorValueInfo> list = new ArrayList<FactorValueInfo>();
            list.addAll(fvMap.values());

            Collections.sort(list, new Comparator<FactorValueInfo>() {
                public int compare(FactorValueInfo o1, FactorValueInfo o2) {
                    if (o1.isUp() && !o2.isUp()) {
                        return -1;
                    }

                    if (o2.isUp() && !o1.isUp()) {
                        return 1;
                    }

                    if (o1.isDown() && !o2.isDown()) {
                        return -1;
                    }

                    if (o2.isDown() && !o1.isDown()) {
                        return 1;
                    }

                    Float m1 = o1.maxValue();
                    Float m2 = o2.maxValue();

                    if (m1 == null && m2 == null) {
                        return 0;
                    } else if (m1 == null) {
                        return 1;
                    } else if (m2 == null) {
                        return -1;
                    }

                    return -1 * m1.compareTo(m2);
                }
            });

            int totalNumberOfPoints = numberOfValues;
            ListIterator<FactorValueInfo> iterator = list.listIterator(list.size());
            while (totalNumberOfPoints > MAX_POINTS_IN_THUMBNAIL && iterator.hasPrevious()) {
                FactorValueInfo info = iterator.previous();
                if (!info.isUpOrDown()) {
                    iterator.remove();
                    totalNumberOfPoints -= info.size();
                } else {
                    break;
                }
            }

            List<Object> seriesList = new ArrayList<Object>();
            List<List<Number>> meanSeriesData = new ArrayList<List<Number>>();

            int position = 0;
            int insignificantSeries = 0;

            double reduceFactor = totalNumberOfPoints > 0 ? (1.0 * MAX_POINTS_IN_THUMBNAIL) / totalNumberOfPoints : 0.0;

            for (FactorValueInfo fvInfo : list) {

                fvInfo.reduce(reduceFactor);

                if (fvInfo.isEmpty()) {
                    continue;
                }

                List<Object> seriesData = new ArrayList<Object>();

                double meanValue = 0;
                int meanCount = 0;
                int startPosition = position;

                for (AssayInfo assayInfo : fvInfo.assays) {
                    Float value = assayInfo.expression;
                    // TODO Why -1E+6 threshold is used here?
                    seriesData.add(Arrays.<Number>asList(position++, value <= -1E+6 ? null : value));
                    if (value > -1E+6) {
                        meanValue += value;
                        meanCount++;
                    }
                }

                if (meanCount > 0) {
                    meanValue /= meanCount;

                    meanSeriesData.add(Arrays.<Number>asList(startPosition, meanValue));
                    meanSeriesData.add(Arrays.<Number>asList(position - 1, meanValue));
                }

                Map<String, Object> series = makeMap(
                        "data", seriesData,
                        "bars", makeMap("show", true, "align", "center", "fill", true),
                        "lines", makeMap("show", false),
                        "points", makeMap("show", false),
                        "label", fvInfo.name,
                        "legend", makeMap("show", true)
                );

                series.put("pvalue", fvInfo.pValue);
                series.put("expression", (fvInfo.isUp() ? "up" : (fvInfo.isDown() ? "dn" : "no")));

                if (!fvInfo.isInsignificant) {
                    series.put("color", altColors[insignificantSeries++ % 2]);
                    series.put("legend", makeMap("show", false));
                }

                seriesList.add(series);
            }

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

            Map<String, Object> options = makeMap(
                    "xaxis", makeMap("ticks", 0),
                    "legend", makeMap("show", true,
                    "position", "sw",
                    "insigLegend", makeMap("show", insignificantSeries > 0),
                    "noColumns", 1),
                    "grid", makeMap(
                    "backgroundColor", "#fafafa",
                    "autoHighlight", false,
                    "hoverable", true,
                    "clickable", true,
                    "borderWidth", 1));

            options.putAll(addToOptions);

            return makeMap(
                    "series", seriesList,
                    "options", options
            );
        }
    }

    /**
     * @param geneId
     * @param ef           experimental factor being plotted
     * @param efvClickedOn clicked on by the user on gene page. If non-null, its best Expression Analysis
     *                     will determine which proxy to draw expression data from.
     * @param efvToBestEA  Map: efv -> best EA, for this ef
     *                     All efv keys in this map will be plotted
     * @return Map key -> value representing a single plot
     * @throws IOException
     */
    private Map<String, Object> createBarPlot(
            Long geneId,
            String ef,
            String efvClickedOn,
            final Map<String, ExpressionAnalysis> efvToBestEA,
            final String experimentAccession)
            throws IOException {

        if (efvToBestEA.containsKey(EMPTY_EFV)) {
            // Don't plot (empty) efvs unless they are the only efv that could be plotted
            efvToBestEA.remove(EMPTY_EFV);
        }
        Set<String> efvsToPlot = efvToBestEA.keySet();

        log.debug("Creating plot... EF: {}, Top FVs: [{}], Best EAs: [{}]",
                new Object[]{ef, StringUtils.join(efvsToPlot, ","), efvToBestEA});

        String bestProxyId;
        if (efvClickedOn != null && efvToBestEA.get(efvClickedOn) != null) {
            // If the user has clicked on an efv, choose to plot expression data from NetCDF proxy in which
            // the best pValue for this proxy occurred.
            bestProxyId = efvToBestEA.get(efvClickedOn).getProxyId();
        } else { // The user hasn't clicked on an efv - choose the proxy in most besEA across all efvs
            bestProxyId = getBestProxyId(efvToBestEA.values());
        }

        NetCDFProxy proxy = null;
        try {
            proxy = atlasNetCDFDAO.getNetCDFProxy(experimentAccession, bestProxyId);

            // Find array design accession for bestProxyId - this will be displayed under the plot
            String arrayDesignAcc = proxy.getArrayDesignAccession();
            String arrayDesignName = atlasDatabaseDAO.getArrayDesignShallowByAccession(arrayDesignAcc).getName();
            String arrayDesignDescription = arrayDesignAcc + (arrayDesignName != null ? " " + arrayDesignName : "");

            // Find best pValue expressions for geneId and ef in bestProxyId - it's expression values for these
            // that will be plotted
            Map<String, ExpressionAnalysis> bestEAsPerEfvInProxy =
                    atlasNetCDFDAO.getBestEAsPerEfvInProxy(experimentAccession, bestProxyId, geneId, ef);

            BarPlotDataBuilder barPlotData = new BarPlotDataBuilder(proxy.getFactorValues(ef));


            for (String factorValue : barPlotData.getUniqueFactorValues()) {
                ExpressionAnalysis bestEA = bestEAsPerEfvInProxy.get(factorValue);

                if (bestEA == null) {
                    // If no bestEA expression analysis for factorValue could be found in proxy
                    // (e.g. factorValue is present, but only with pVal == 0) then don't
                    // plot this factorValue for proxyId
                    barPlotData.removeFactorValue(factorValue);
                    continue;
                }

                // Get the actual expression data from the proxy-designindex corresponding to the best pValue
                List<Float> expressions = atlasNetCDFDAO.getExpressionData(experimentAccession, bestProxyId, bestEA.getDesignElementIndex());

                barPlotData.setExpressions(factorValue, expressions);
                barPlotData.setPValue(factorValue, bestEA.getPValAdjusted());
                barPlotData.setUpDown(factorValue, bestEA.isNo() ? null : bestEA.isUp());
                barPlotData.setInsignificant(factorValue, efvsToPlot.contains(factorValue));
                if (!efvsToPlot.contains(factorValue))
                    log.debug(experimentAccession + ": Factor value: " + factorValue + " not present in efvsToPlot (" + StringUtils.join(efvsToPlot, ",") + "), " +
                            "flagging this series insignificant");
            }

            Map<String, Object> options = makeMap(
                    "arrayDesign", arrayDesignDescription,
                    "ef", ef);

            return barPlotData.toSeries(options);
        } finally {
            closeQuietly(proxy);
        }
    }


    private Map<String, Object> createThumbnailPlot(String ef, String efv, ExpressionAnalysis ea, String experimentAccession)
            throws IOException {
        log.debug("Creating thumbnail plot... EF: {}, Top FVs: {}, ExpressionAnalysis: {}",
                new Object[]{ef, efv, ea});

        List<Object> seriesData = new ArrayList<Object>();
        int startMark = 0;
        int endMark = 0;
        // Get assayFVs from the proxy from which ea came
        List<String> assayFVs = atlasNetCDFDAO.getFactorValues(experimentAccession, ea.getProxyId(), ef);
        List<String> uniqueFVs = sortUniqueFVs(assayFVs);
        // Get actual expression data from the design element stored in ea
        List<Float> expressions = atlasNetCDFDAO.getExpressionData(experimentAccession, ea.getProxyId(), ea.getDesignElementIndex());


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

    private static List<String> sortUniqueFVs(Collection<String> assayFVs) {
        Set<String> uniqueSet = new HashSet<String>(assayFVs);
        List<String> uniqueFVs = new ArrayList<String>(uniqueSet);
        Collections.sort(uniqueFVs, new Comparator<String>() {
            public int compare(String s1, String s2) {
                // want to make sure that empty strings are pushed to the back
                boolean isEmptyS1 = (s1.length() == 0);
                boolean isEmptyS2 = (s2.length() == 0);

                if (isEmptyS1 && isEmptyS2) {
                    return 0;
                }
                if (isEmptyS1) {
                    return 1;
                }
                if (isEmptyS2) {
                    return -1;
                }

                java.util.regex.Matcher m1 = startsOrEndsWithDigits.matcher(s1);
                java.util.regex.Matcher m2 = startsOrEndsWithDigits.matcher(s2);

                if (m1.find() && m2.find()) {
                    Long i1 = new Long(s1.substring(m1.start(), m1.end()));
                    Long i2 = new Long(s2.substring(m2.start(), m2.end()));

                    int compareRes = i1.compareTo(i2);
                    return (compareRes == 0) ? s1.compareToIgnoreCase(s2) : compareRes;
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
        for (Map<String, ExpressionAnalysis> efvToEa : efToEfvToEA.values()) {
            for (ExpressionAnalysis ea : efvToEa.values()) {
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

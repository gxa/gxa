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
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.primitives.Floats;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.data.ExperimentPartCriteria.experimentPart;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

public class AtlasPlotter {
    private AtlasDataDAO atlasDataDAO;
    private AtlasDAO atlasDatabaseDAO;
    private GeneSolrDAO geneSolrDAO;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String[] altColors = {"#D8D8D8", "#E8E8E8"};

    private static final Pattern startsOrEndsWithDigits = java.util.regex.Pattern.compile("^\\d+|\\d+$");

    private static final int MAX_POINTS_IN_THUMBNAIL = 500;

    public void setAtlasDatabaseDAO(AtlasDAO atlasDatabaseDAO) {
        this.atlasDatabaseDAO = atlasDatabaseDAO;
    }

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }


    public void setAtlasDataDAO(AtlasDataDAO atlasDataDAO) {
        this.atlasDataDAO = atlasDataDAO;
    }

    public Map<String, Object> getGeneInExpPlotData(final String geneIdKey,
                                                    final Experiment experiment,
                                                    final String ef,
                                                    final String efv) {

        log.debug("Plotting gene {}, experiment {}, factor {}", new Object[]{geneIdKey, experiment, ef});

        try {
            final List<AtlasGene> genes = parseGenes(geneIdKey);

            // geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
            // Note that ea contains arrayDesign accession and designElement index from which it came, so that
            // the actual expression values can be easily retrieved later
            final Collection<Long> geneIds = transform(genes, new Function<AtlasGene, Long>() {
                public Long apply(@Nonnull AtlasGene input) {
                    return (long) input.getGeneId();
                }
            });
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    getGeneIdsToEfToEfvToEA(experiment, ef, efv, geneIds);
            if (geneIdsToEfToEfvToEA == null) {
                return null;
            }

            String efToPlot = isNullOrEmpty(ef) ?
                    getHighestRankEF(geneIdsToEfToEfvToEA.get(
                            (long) genes.get(0).getGeneId()
                    )) : ef;

            if (efToPlot == null)
                throw LogUtil.createUnexpected("Can't find EF to plot");

            AtlasGene geneToPlot = genes.get(0);
            Long geneId = (long) geneToPlot.getGeneId();
            Map<String, ExpressionAnalysis> efvToBestEA = geneIdsToEfToEfvToEA.get(geneId).get(efToPlot);
            if (!efvToBestEA.isEmpty())
                return createBarPlot(geneId, efToPlot, efv, efvToBestEA, experiment);

        } catch (AtlasDataException e) {
            throw createUnexpected("AtlasDataException whilst trying to read data for experiment " + experiment, e);
        }
        return null;
    }

    private Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getGeneIdsToEfToEfvToEA(Experiment experiment, String ef, String efv, Collection<Long> geneIds) throws AtlasDataException {
        if (geneIds == null || geneIds.isEmpty() || isNullOrEmpty(ef)) {
            return null;
        }

        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(experiment);
        try {
            ExperimentPart expPart = experimentPart()
                    .containsGenes(geneIds)
                    .containsEfEfv(ef, efv)
                    .retrieveFrom(ewd);
            return expPart == null ? null : expPart.getExpressionAnalysesForGeneIds(geneIds);
        } catch (StatisticsNotFoundException e) {
            return null;
        } finally {
            closeQuietly(ewd);
        }
    }

    private List<AtlasGene> parseGenes(String geneIdKey) {
        List<AtlasGene> genes = new ArrayList<AtlasGene>();

        // lookup gene names, again using SOLR index
        for (String geneIdStr : geneIdKey.split(",")) {
            if (Strings.isNullOrEmpty(geneIdStr))
                continue;
            GeneSolrDAO.AtlasGeneResult gene = geneSolrDAO.getGeneById(Long.parseLong(geneIdStr));
            if (gene.isFound()) {
                AtlasGene atlasGene = gene.getGene();
                genes.add(atlasGene);
            }
        }
        if (genes.isEmpty()) {
            throw LogUtil.createUnexpected("No existing genes specified by query: geneIdKey = '" + geneIdKey + "'");
        }

        return genes;
    }

    /**
     * @param eas
     * @return list of proxy ids in eas
     */
    private List<String> getArrayDesigns(Collection<ExpressionAnalysis> eas) {
        List<String> adAccessions = new ArrayList<String>();
        for (ExpressionAnalysis ea : eas) {
            adAccessions.add(ea.getArrayDesignAccession());
        }
        return adAccessions;
    }

    private String getMostFrequent(List<String> values) {
        final Set<String> uniqueValues = new HashSet<String>(values);
        int maxFrequency = 0;
        String bestValue = null;
        for (String v : uniqueValues) {
            final int freq = Collections.frequency(values, v);
            if (freq > maxFrequency) {
                bestValue = v;
                maxFrequency = freq;
            }
        }
        return bestValue;
    }

    protected static class FactorValueInfo {
        private final String name;
        private final UpDownExpression upDown;
        private final Float pValue;
        private final List<Float> assayValues = new ArrayList<Float>();
        private final boolean isInsignificant;

        public FactorValueInfo(@Nonnull String name, @Nonnull UpDownExpression upDown, @Nonnull Float pValue, boolean isInsignificant) {
            this.name = name;
            this.upDown = upDown;
            this.pValue = pValue;
            this.isInsignificant = isInsignificant;
        }

        public void setAssayValues(Collection<Float> assayValues) {
            this.assayValues.addAll(assayValues);

            Collections.sort(this.assayValues, new Comparator<Float>() {
                public int compare(Float e1, Float e2) {
                    return -1 * compareExpressions(e1, e2);
                }

                private int compareExpressions(Float e1, Float e2) {
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

        public Collection<Float> getAssayValues() {
            return Collections.unmodifiableCollection(assayValues);
        }

        public boolean isUp() {
            return upDown.isUp();
        }

        public boolean isDown() {
            return upDown.isDown();
        }

        public boolean isUpOrDown() {
            return isUp() || isDown();
        }

        public Float maxValue() {
            if (assayValues.isEmpty()) {
                throw new IllegalStateException("Can't get max value for an empty list of assays");
            }
            return assayValues.get(0);
        }

        public boolean isEmpty() {
            return assayValues.isEmpty();
        }

        public void reduce(double reduceFactor) {
            int n = (int) Math.floor(reduceFactor * assayValues.size());
            if (n >= assayValues.size()) {
                return;
            }

            List<Float> list = new ArrayList<Float>();

            if (n > 0 && !assayValues.isEmpty()) {
                float max = assayValues.get(0);
                float min = assayValues.get(assayValues.size() - 1);
                float dv = Math.abs((max - min) / n);

                float v = max;
                float prevDx = 0;
                Float prev = null;

                for (Float x : assayValues) {
                    float dx = Math.abs(x - v);
                    if (prev != null && dx > prevDx) {
                        list.add(prev);
                        v -= dv;
                    }
                    prevDx = dx;
                    prev = x;
                }
            }

            assayValues.clear();
            assayValues.addAll(list);
        }

        public int size() {
            return assayValues.size();
        }
    }

    protected static class AssayFactorValues {

        private Multimap<String, Integer> fvMap = Multimaps.newListMultimap(
                Maps.<String, Collection<Integer>>newHashMap(),
                new Supplier<List<Integer>>() {
                    @Override
                    public List<Integer> get() {
                        return Lists.newArrayList();
                    }
                }
        );

        public AssayFactorValues(String[] allFactorValues) {
            for (int i = 0; i < allFactorValues.length; i++) {
                String factorValue = allFactorValues[i];
                fvMap.put(factorValue, i);
            }
        }

        public Collection<String> getUniqueValues() {
            return Collections.unmodifiableCollection(fvMap.keySet());
        }

        public Collection<Float> getAssayExpressionsFor(String factorValue, List<Float> expressions) {
            List<Float> assays = new ArrayList<Float>();
            Collection<Integer> assayIndices = fvMap.get(factorValue);
            for (Integer i : assayIndices) {
                if (expressions.size() <= i) {
                    throw new IllegalArgumentException("No expression for assayIndex: " + i + ", assayExpressions.size = " + expressions.size());
                }
                assays.add(expressions.get(i));
            }
            return assays;
        }
    }

    private static class BarPlotDataBuilder {
        private List<FactorValueInfo> factorValues = new ArrayList<FactorValueInfo>();
        private int numberOfValues = 0;

        private void addFactorValue(String fv, ExpressionAnalysis bestEA, boolean isInsignificant, Collection<Float> assayValues) {
            FactorValueInfo fvInfo = new FactorValueInfo(fv, bestEA.getExpression(), bestEA.getP(), isInsignificant);
            fvInfo.setAssayValues(assayValues);
            numberOfValues += assayValues.size();
            factorValues.add(fvInfo);
        }

        public Map<String, Object> toSeries(Map<String, Object> addToOptions) {
            List<FactorValueInfo> list = new ArrayList<FactorValueInfo>();
            list.addAll(factorValues);

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

                for (Float value : fvInfo.assayValues) {
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
     * @throws AtlasDataException
     */
    private Map<String, Object> createBarPlot(
            Long geneId,
            String ef,
            String efvClickedOn,
            final Map<String, ExpressionAnalysis> efvToBestEA,
            final Experiment experiment)
            throws AtlasDataException {

        Set<String> efvsToPlot = efvToBestEA.keySet();

        log.debug("Creating plot... EF: {}, Top FVs: [{}], Best EAs: [{}]",
                new Object[]{ef, StringUtils.join(efvsToPlot, ","), efvToBestEA});

        final String bestArrayDesignAccession;
        if (efvClickedOn != null && efvToBestEA.get(efvClickedOn) != null) {
            // If the user has clicked on an efv, choose to plot expression data in which
            // the best pValue for this proxy occurred.
            bestArrayDesignAccession = efvToBestEA.get(efvClickedOn).getArrayDesignAccession();
        } else { // The user hasn't clicked on an efv - choose the proxy in most besEA across all efvs
            bestArrayDesignAccession = getMostFrequent(getArrayDesigns(efvToBestEA.values()));
        }

        final ArrayDesign ad = new ArrayDesign(bestArrayDesignAccession);
        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(experiment);
        try {

            // Find array design accession for bestProxyId - this will be displayed under the plot
            String arrayDesignName = atlasDatabaseDAO.getArrayDesignShallowByAccession(bestArrayDesignAccession).getName();
            String arrayDesignDescription = bestArrayDesignAccession + (arrayDesignName != null ? " " + arrayDesignName : "");

            final BarPlotDataBuilder barPlotData = new BarPlotDataBuilder();
            // Find best pValue expressions for geneId and ef in bestProxyId - it's expression values for these
            // that will be plotted
            Map<String, ExpressionAnalysis> bestEAsPerEfvInProxy = null;

            try {
                bestEAsPerEfvInProxy = ewd.getBestEAsPerEfvInProxy(ad, geneId, ef);
                final AssayFactorValues factorValues = new AssayFactorValues(ewd.getFactorValues(ad, ef));
                for (String factorValue : factorValues.getUniqueValues()) {
                    ExpressionAnalysis bestEA = bestEAsPerEfvInProxy.get(factorValue);

                    if (bestEA == null) {
                        // If no bestEA expression analysis for factorValue could be found in proxy
                        // (e.g. factorValue is present, but only with pVal == 0) then don't
                        // plot this factorValue for arrayDesign
                        continue;
                    }

                    // Get the actual expression data from the proxy-designindex corresponding to the best pValue
                    final float[] expressions = ewd.getExpressionDataForDesignElementAtIndex(ad, bestEA.getDeIndex());

                    Collection<Float> assays = factorValues.getAssayExpressionsFor(factorValue, Floats.asList(expressions));

                    barPlotData.addFactorValue(
                            factorValue,
                            bestEA,
                            efvsToPlot.contains(factorValue),
                            assays);

                    if (!efvsToPlot.contains(factorValue))
                        log.debug(experiment + ": Factor value: " + factorValue + " not present in efvsToPlot (" + StringUtils.join(efvsToPlot, ",") + "), " +
                                "flagging this series insignificant");
                }
            } catch (StatisticsNotFoundException e) {
                // ignore
            }

            final Map<String, Object> options = makeMap(
                    "arrayDesign", arrayDesignDescription,
                    "ef", ef);

            return barPlotData.toSeries(options);
        } finally {
            closeQuietly(ewd);
        }
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
                if (bestPValue == null || bestPValue > ea.getP()
                        || (bestPValue == ea.getP() && bestTStat < ea.getT())) {
                    bestEf = ea.getEfv().getFirst();
                    bestPValue = ea.getP();
                    bestTStat = ea.getT();
                }
            }
        }
        return bestEf;
    }
}

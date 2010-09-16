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
import uk.ac.ebi.gxa.utils.CountIterator;
import uk.ac.ebi.gxa.utils.FilterIterator;
import uk.ac.ebi.gxa.utils.FlattenIterator;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.File;
import java.io.FilenameFilter;
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
    private static final String EMTPY_EFV = "(empty)";


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

            String efToPlot;

            // if ef is "default" fetch highest ranked EF using SOLR index
            if ("default".equals(ef)) {
                efToPlot = genes.get(0).getHighestRankEF(Long.valueOf(experimentID)).getFirst();
            }
            else {
                efToPlot = ef;
            }

            if (efToPlot == null)
                throw new RuntimeException("Can't find EF to plot");


            // geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
            // Note that ea contains proxyId and designElement index from which it came, so that
            // the actual expression values can be easily retrieved later
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                    atlasNetCDFDAO.getExpressionAnalysesForGeneIds(geneIds, experimentID);

            if (plotType.equals("thumb")) {
                AtlasGene geneToPlot = genes.get(0);
                Long geneId = Long.parseLong(geneToPlot.getGeneId());
                ExpressionAnalysis bestEA = geneIdsToEfToEfvToEA.get(geneId).get(efToPlot).get(efv);
                if (bestEA == null)
                    throw new RuntimeException("Can't find deIndex for min pValue for gene " + geneIdKey);
                return createThumbnailPlot(efToPlot, efv, bestEA);
            } else if (plotType.equals("large")) {
                // Find first proxy for this experiment that has ef
                // TODO make createLargePlot() work with multiple proxies
                proxy = atlasNetCDFDAO.findFirstProxyForGenes(experimentID, geneIds);
                assert(proxy != null); // At least one proxy for this experiment should contain geneIds
                return createLargePlot(proxy, efToPlot, genes, geneIdsToEfToEfvToEA, experimentID);
            } else {
                AtlasGene geneToPlot = genes.get(0);
                Long geneId = Long.parseLong(geneToPlot.getGeneId());
                Map<String, ExpressionAnalysis> efvToBestEA = geneIdsToEfToEfvToEA.get(geneId).get(efToPlot);
                return createBarPlot(efToPlot, efvToBestEA, experimentID);
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
     * @param efv
     * @param efvsToPvaluesGeneIndexes Map: efv -> (Map: pValue -> (Map: proxy Id  -> geneIndex))
     * @return (Map: proxyId  -> geneIndex) for the minimum pValue for efv
     */
    private Map<String, Integer> getBestGeneIndexForEfv(
            final String efv,
            final Map<String, TreeMap<Float, Map<String, Integer>>> efvsToPvaluesGeneIndexes) {
        TreeMap<Float, Map<String, Integer>> pValueToGeneIndexForEfv = efvsToPvaluesGeneIndexes.get(efv);
        if (pValueToGeneIndexForEfv == null) {
            // No pValues were found for this efv in any netCDF proxy that contained its ef
            return null;
        }
        Float minPValueForEfv = pValueToGeneIndexForEfv.firstKey();
        return pValueToGeneIndexForEfv.get(minPValueForEfv);
    }

    /**
     * @param ef  experimental factor being plotted
     * @param efvToBestEA Map: efv -> best EA, for this ef
     *                    All efv keys in this map will be plotted
     * @return
     * @throws IOException
     */
    private Map<String, Object> createBarPlot(
            String ef,
            final Map<String, ExpressionAnalysis> efvToBestEA,
            final String experimentID)
            throws IOException {

        // Assemble best gene indexes for ef
        Set<String> efvsToPlot = efvToBestEA.keySet();
        // Don't plot (empty) efvs
        if (efvsToPlot.contains(EMTPY_EFV)) {
            efvsToPlot.remove(EMTPY_EFV);
        }
        log.debug("Creating plot... EF: {}, Top FVs: [{}], Best EAs: [{}]",
                new Object[]{ef, StringUtils.join(efvsToPlot, ","), efvToBestEA});

        List<Object> seriesList = new ArrayList<Object>();
        boolean insignificantSeries = false;
        // data for mean series
        List<List<Number>> meanSeriesData = new ArrayList<List<Number>>();

        // get unique factor values for ef in this experiment
        List<String> uniqueFVs = atlasNetCDFDAO.getUniqueFactorValues(experimentID, ef);

        // Map; proxy id to the factor values actually occuring in this proxy; used later
        // when plotting factors form the proxy in which bestEA (i.e lowest pValue) was found.
        Map<String, List<String>> proxyIdToFVs =
                atlasNetCDFDAO.getFactorValuesForExperiment(experimentID, ef);

        int counter = 0;
        int position = 0;
        for (String factorValue : uniqueFVs) {
            // create a series for these datapoints - new series for each factor value
            List<Object> seriesData = new ArrayList<Object>();

            ExpressionAnalysis bestEA = efvToBestEA.get(factorValue);

            if (bestEA == null) {
                // If no proxy could be found for gene->ef->efv, that means that NetDCFs for experimentID
                // contain ef-efv combination but not for this gene -
                // we don't plot thsi factorValue for this gene
                continue;
            }
            // Get assayFVs from the proxy from which bestEA came
            List<String> assayFVs = proxyIdToFVs.get(bestEA.getProxyId());
            // Get the actual expression data from the proxy-designindex corresponding to the best pValue
            List<Float> expressions = atlasNetCDFDAO.getExpressionData(bestEA.getProxyId(), bestEA.getDesignElementIndex());

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
            if (!efvsToPlot.contains(factorValue.toLowerCase())) {
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
                                "borderWidth", 1)
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
     * @param geneIdsToEfToEfvToEA Map: geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     * Note that ea contains proxyId and designElement index from which it came, so that
     * the actual expression values can be easily retrieved later.
     *
     * @return
     * @throws IOException
     */
    private Map<String,Object> createLargePlot(final NetCDFProxy netCDF,
                                               final String ef,
                                               final List<AtlasGene> genes,
                                               final Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA,
                                               final String experimentID)
            throws IOException {

        log.debug("Creating big plot... EF: {}, Gene Names: [{}]",
                  new Object[]{ef, StringUtils.join(genes, " ")});

        // data for individual series
        List<Object> seriesList = new ArrayList<Object>();


        List<String> assayFVs = Arrays.asList(netCDF.getFactorValues(ef));
        // Don't plot (empty) efvs
        if (assayFVs.contains(EMTPY_EFV)) {
            assayFVs.remove(EMTPY_EFV);
        }
        List<String> uniqueFVs = sortUniqueFVs(assayFVs);

        // Map: geneId -> bestEA (the one with the lowest pValue) EA across oall efvs for this ef
        final Map<String, ExpressionAnalysis> geneIdToBestEAAcrossAllEfvs =
                new HashMap<String, ExpressionAnalysis>();

        // iterate over design elements axis
        // fixme: this assumes gene indices and design element indices are the same, actually we need to do a lookup
        for (AtlasGene gene : genes) {

            // Find ExpressionAnalysis with the lowest pValue across all efvs
            Long geneId = Long.parseLong(gene.getGeneId());
            Map<String, ExpressionAnalysis> efvToBestEA = geneIdsToEfToEfvToEA.get(geneId).get(ef);
            ExpressionAnalysis bestEAAcrossAllEfvs = null;
            for (ExpressionAnalysis ea : efvToBestEA.values()) {
                if (bestEAAcrossAllEfvs == null ||
                        bestEAAcrossAllEfvs.getPValAdjusted() > ea.getPValAdjusted()) {
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

        List<Map> markings = new ArrayList<Map>();
        int position = 0;
        int flicker = 0;
        Integer[] sortedAssayOrder = new Integer[assayFVs.size()];
        for(String factorValue : uniqueFVs) {
            int start = position;
            int assayIndex = 0;
            for(String assayFV : assayFVs) {
                if(assayFV.equals(factorValue)) {
                    sortedAssayOrder[position] = assayIndex;
                    ++position;
                }
                ++assayIndex;
            }

            markings.add(makeMap(
                    "xaxis", makeMap("from", start, "to", position),
                    "label", factorValue.length() > 0 ? factorValue : "unannotated",
                    "color", markingColors[++flicker % 2]
                    ));
        }


        final Map<String, List<Long>> proxyIdToDesignElements =
                atlasNetCDFDAO.getProxyIdToDesignElements(experimentID);
        final Map<String, Long> proxyIdToArrayDesignId =
                atlasNetCDFDAO.getProxyIdToArrayDesignId(experimentID);

        final List<String> efs = Arrays.asList(netCDF.getFactors());
        final List<String> scs = Arrays.asList(netCDF.getCharacteristics());
        final int[][] bs2as = netCDF.getSamplesToAssays();

        final Map<String, String[]> efvs = new HashMap<String, String[]>();
        for(String i : efs)
            efvs.put(i, netCDF.getFactorValues(i));

        final Map<String, String[]> scvs = new HashMap<String, String[]>();
        for(String i : scs)
            scvs.put(i, netCDF.getCharacteristicValues(i));

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
                "assayOrder", Arrays.asList(sortedAssayOrder).iterator(),
                "assayProperties", new MappingIterator<Integer,Map>(Arrays.asList(sortedAssayOrder).iterator()) {
                    public Map map(final Integer assayIndex) {
                        return makeMap(
                                "efvs", new FilterIterator<String,Map>(efs.iterator()) {
                                    public Map map(String ef) {
                                        String v = efvs.get(ef)[assayIndex];
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
                                                String v = scvs.get(sc)[sampleIndex];
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
}

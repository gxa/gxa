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
    private File atlasNetCDFRepo;
    private AtlasDAO atlasDatabaseDAO;
    private AtlasSolrDAO atlasSolrDAO;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String[] altColors = {"#D8D8D8", "#E8E8E8"};
    private static final String[] markingColors = {"#F0FFFF", "#F5F5DC"};
    private static final Pattern startsOrEndsWithDigits = java.util.regex.Pattern.compile("^\\d+|\\d+$");


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

    public File getAtlasNetCDFRepo() {
        return atlasNetCDFRepo;
    }

    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }


    /**
     * @param experimentID experiment for which the data should be added
     * @param proxy NetCDF to retrieve data from for geneids
     * @param genesToEfvsPvaluesSoFar  Map: geneId -> (Map: efv -> (TreeMap: pValue -> Map: netCDF proxy Id -> geneIndex))
     * keySet() of the TreeMap is sorted in ASC natural order of its keys, which in this case means that
     * firstKey() method call returns the minimum pValue for the efv pointing to this TreeMap. This minimum pValue
     * (itself a key) can then be used to access the value it points to: a Map: proxy id -> geneIndex of the min pValue, found in
     * that proxy. We need to store proxy id as well as the geneIndex so that we know which proxy to retrieve Expression stats data from.
     * @param geneIds for which efvs and their pValue -> (Map: netCDF proxy Id -> geneIndex) mappings are to be stored in genesToEfvsPvaluesSoFar
     * @throws IOException
     */
    private void addGeneIndexesForMinPValuesForEfvsFromProxy(
            final Long experimentID,
            final NetCDFProxy proxy,
            final Set<Long> geneIds,
            Map<Long, Map<String, TreeMap<Float, Map<String, Integer>>>> genesToEfvsPvaluesSoFar) throws IOException {

        final String proxyId = proxy.getId();

        // Get all gene ids from proxy
        final long[] allGeneIds = proxy.getGenes();
        final long[] deIds = proxy.getDesignElements();

        // Store gene indexes for all genes in geneIds
        List<Integer> geneIndexes = new ArrayList<Integer>();
        int geneIndex = 0;
        for (Long geneId : allGeneIds) {
            if (geneIds.contains(geneId)) {
                geneIndexes.add(geneIndex);
            }
            geneIndex++;
        }

        // Add retrieved pValues for each geneId-efv combination
        Map<Integer, List<ExpressionAnalysis>> deIndexToEas =
                proxy.getExpressionAnalysesForDesignElementIndexes(geneIndexes, deIds);
        for (Integer deIndex : deIndexToEas.keySet()) {
            Long geneId = allGeneIds[deIndex];
            for (ExpressionAnalysis ea : deIndexToEas.get(deIndex)) {
                if (ea.getExperimentID() == experimentID) {
                    addGeneIndexIfMinPValueForGeneEFV(
                            proxyId,
                            geneId,
                            deIndex,
                            ea.getEfvName().toLowerCase(),
                            ea.getPValAdjusted(),
                            genesToEfvsPvaluesSoFar);
                }
            }
        }
    }

    /**
     * "param proxyId in which deIndex for min pValues was found
     * @param geneId for which Map: geneId -> (Map: efv -> (TreeMap: pValue -> Map: netCDF proxy Id -> geneIndex))
     * mapping needs to be stored in genesToEfvsPvaluesSoFar
     * @param deIndex (aka geneIndex. as we have 1:1 correspondence between design elements and genes)
     * @param efv
     * @param newPVal
     * @param genesToEfvsPvaluesSoFar Map: Map: geneId -> (Map: efv -> (TreeMap: pValue -> Map: netCDF proxy Id -> geneIndex))
     */
    private void addGeneIndexIfMinPValueForGeneEFV(String proxyId,
                                                   Long geneId,
                                                   Integer deIndex,
                                                   String efv,
                                                   Float newPVal,
                                                   Map<Long, Map<String, TreeMap<Float, Map<String, Integer>>>> genesToEfvsPvaluesSoFar) {
        if (!genesToEfvsPvaluesSoFar.keySet().contains(geneId)) {
            genesToEfvsPvaluesSoFar.put(geneId, new HashMap<String, TreeMap<Float, Map<String,Integer>>>());
        }
        if (!genesToEfvsPvaluesSoFar.get(geneId).keySet().contains(efv)) {
            genesToEfvsPvaluesSoFar.get(geneId).put(efv, new TreeMap<Float, Map<String,Integer>>());
        }
        Map<String, Integer> proxyIdToDeIndex = new HashMap<String, Integer>();
        proxyIdToDeIndex.put(proxyId, deIndex);
        genesToEfvsPvaluesSoFar.get(geneId).get(efv).put(newPVal, proxyIdToDeIndex);
    }


    public Map<String,Object> getGeneInExpPlotData(final String geneIdKey,
                                    final String experimentID,
                                    final String ef,
                                    final String efv,
                                    final String plotType) {

        log.debug("Plotting gene {}, experiment {}, factor {}", new Object[]{geneIdKey, experimentID, ef});

        // lookup NetCDFFiles for this experiment
        File[] netCDFs = atlasNetCDFRepo.listFiles(new FilenameFilter() {

            public boolean accept(File file, String name) {
                return name.matches("^" + experimentID + "_[0-9]+(_ratios)?\\.nc$");
            }
        });

        // create NetCDFProxies for each of these files, to find the gene we want
        List<NetCDFProxy> proxies = new ArrayList<NetCDFProxy>();
        for (File netCDF : netCDFs) {
            proxies.add(new NetCDFProxy(netCDF));
        }

        // iterate over our proxies to find the one that contains the genes we're after
        try {
            List<AtlasGene> genes = new ArrayList<AtlasGene>();
            Set<Long> geneIds = new HashSet<Long>();

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

            /* Map: geneId -> (Map: efv -> (TreeMap: pValue -> Map: netCDF proxy Id -> geneIndex))
             * keySet() of the TreeMap is sorted in ASC natural order of its keys, which in this case means that
             * firstKey() method call returns the minimum pValue for the efv pointing to this TreeMap. This minimum pValue
             * (itself a key) can then be used to access the value it points to: a Map: proxy id -> geneIndex of the min pValue, found in
             * that proxy. We need to store proxy id as well as the geneIndex so that we know which proxy to retrieve Expression stats data from.
             *
             * geneIndex is used later to retrieve ExpressionAnalytics data corresponding to the pValue pointing to it.
             * */
            Map<Long, Map<String, TreeMap<Float, Map<String,Integer>>>> genesToEfvsPvalues =
                    new HashMap<Long, Map<String, TreeMap<Float, Map<String, Integer>>>>();
            NetCDFProxy proxy = null;
            // this is a list of the indices marking the positions of design elements that correspond to the gene we're after
            Long experimentId = Long.parseLong(experimentID);
            for (NetCDFProxy next : proxies) {
                if (!Arrays.asList(next.getFactors()).contains(efToPlot))
                    continue;
                // Add efv -> pValue -> geneIndex mappings to genesToEfvsPvalues, for each geneid in geneIds                
                addGeneIndexesForMinPValuesForEfvsFromProxy(experimentId, next, geneIds, genesToEfvsPvalues);
                if (genesToEfvsPvalues.keySet().size() > 0 && proxy == null) {
                    // createLargePlot() currently cannot cope with more than one proxy - we pass to it the first
                    // one in which we find the experimental factor of interest (ef). Since one ef may have
                    // its corresponding ef values distributed across multiple NetCDFs, as things stand now the large plot
                    // will not display ef values that are not in this first proxy.
                    // TODO make createLargePlot() work with multiple proxies
                    proxy = next;
                }
            }

            if (plotType.equals("thumb")) {
                AtlasGene geneToPlot = genes.get(0);
                TreeMap<Float, Map<String,Integer>> pValueToGeneIndex = genesToEfvsPvalues.get(geneToPlot.getGeneId()).get(efv);
                Float minPValue = pValueToGeneIndex.firstKey();
                if (minPValue == null)
                    throw new RuntimeException("Failed to find minimum pValue for gene " + geneToPlot.getGeneId());
                Map<String,Integer> proxyIdTodeIndexForMinPValue = pValueToGeneIndex.get(minPValue);
                String proxyId = proxyIdTodeIndexForMinPValue.keySet().iterator().next(); // we expect only one proxyId key in this map
                Integer deIndexForMinPValue = proxyIdTodeIndexForMinPValue.get(proxyId);
                if (deIndexForMinPValue == null)
                    throw new RuntimeException("Can't find deIndex for min pValue for gene " + geneIdKey);
                return createThumbnailPlot(proxies, efToPlot, efv, deIndexForMinPValue);
            } else if (plotType.equals("large")) {
                return createLargePlot(proxy, efToPlot, genes, genesToEfvsPvalues);
            } else {
                AtlasGene geneToPlot = genes.get(0);
                Long geneId = Long.parseLong(geneToPlot.getGeneId());
                return createBarPlot(proxies, efToPlot, genesToEfvsPvalues.get(geneId));
            }

        } catch (IOException e) {
            log.error("IOException whilst trying to read from NetCDF at " + atlasNetCDFRepo.getAbsolutePath() +
                    " for " + experimentID);
            throw new RuntimeException("IOException whilst trying to read from NetCDF for "
                    + atlasNetCDFRepo.getAbsolutePath(), e);
        } finally {
            for(NetCDFProxy proxy : proxies) {
                try {
                    if(null != proxy)
                        proxy.close();
                } catch (IOException e) {
                    log.error("Failed to close NetCDF proxy", e);
                }
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
     *
     * @param netCDFs netCDF proxies to retrieve data from
     * @param ef experimental factor being plotted
     * @param efvsToPvaluesGeneIndexes Map: efv -> (Map: pValue -> (Map: proxy Id -> geneIndex)), used to retrieve expression stats for each efv key
     * All efv keys in this map will be plotted
     * @return
     * @throws IOException
     */
    private Map<String, Object> createBarPlot(
            final List<NetCDFProxy> netCDFs, String ef,
            final Map<String, TreeMap<Float, Map<String, Integer>>> efvsToPvaluesGeneIndexes)
            throws IOException {

        // Assemble best gene indexes for ef
        Set<String> efvsToPlot = efvsToPvaluesGeneIndexes.keySet();
        Map<String, Map<String, Integer>> efvsToBestGeneIndex = new HashMap<String, Map<String, Integer>>();
        for (String efv : efvsToPvaluesGeneIndexes.keySet()) {
            Map<String, Integer> bestGeneIndex = getBestGeneIndexForEfv(efv, efvsToPvaluesGeneIndexes);
            if (bestGeneIndex != null) {
                efvsToBestGeneIndex.put(efv, bestGeneIndex);
            }
        }
        log.debug("Creating plot... EF: {}, Top FVs: [{}], DE index: [{}]",
                new Object[]{ef, StringUtils.join(efvsToPlot, ","), efvsToBestGeneIndex});

        List<Object> seriesList = new ArrayList<Object>();
        boolean insignificantSeries = false;
        // data for mean series
        List<List<Number>> meanSeriesData = new ArrayList<List<Number>>();
        // Accumulate data from all NetCDFProxies - as some factors may have their factor values
        // distributed across multiple NetCDFs
        for (NetCDFProxy netCDF : netCDFs) {

            // get unique factor values
            String[] assayFVs = netCDF.getFactorValues(ef);
            String[] uniqueFVs = sortUniqueFVs(assayFVs);

            int counter = 0;
            int position = 0;
            for (String factorValue : uniqueFVs) {
                // create a series for these datapoints - new series for each factor value
                List<Object> seriesData = new ArrayList<Object>();

                Map<String, Integer> proxyIdTobestGeneIndexForEfv = getBestGeneIndexForEfv(factorValue.toLowerCase(), efvsToPvaluesGeneIndexes);
                if (proxyIdTobestGeneIndexForEfv == null) {
                    log.warn("Failed to find min pValue's geneIndex for efv: " + factorValue +
                            " (ef: " + ef + ") in any NetCDF proxy - not plotting this efv!");
                    continue;
                } else {
                    String proxyId = proxyIdTobestGeneIndexForEfv.keySet().iterator().next(); // we expect only one proxyId key in this map
                    if (!proxyId.equals(netCDF.getId())) {
                        log.warn("Skipping this efv: " + factorValue + " for this proxy (id: " + netCDF.getId() + " as the geneIndex found is for another proxy (id: )" + proxyId);
                        continue;
                    }

                    Integer bestGeneIndexForEfv = proxyIdTobestGeneIndexForEfv.get(proxyId);
                    float[] expressions = netCDF.getExpressionDataForDesignElementAtIndex(bestGeneIndexForEfv);

                    double meanForFV = 0;
                    int meanCount = 0;

                    int fvCount = 0;
                    for (int assayIndex = 0; assayIndex < assayFVs.length; assayIndex++)
                        if (assayFVs[assayIndex].equals(factorValue)) {
                            float value = expressions[assayIndex];
                            seriesData.add(Arrays.<Number>asList(++position, value <= -1000000 ? null : value));

                            if (value > -1000000) {
                                meanForFV += value;
                                ++meanCount;
                            }

                            ++fvCount;
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

                    final long[] deIds = netCDF.getDesignElements();
                    List<Integer> geneIndexes = new ArrayList<Integer>();
                    geneIndexes.add(bestGeneIndexForEfv);
                    Map<Integer, List<ExpressionAnalysis>> geneIndexToExpressionAnalyses = netCDF.getExpressionAnalysesForDesignElementIndexes(geneIndexes, deIds);
                    List<ExpressionAnalysis> deIndexToEas = geneIndexToExpressionAnalyses.get(bestGeneIndexForEfv);
                    for (ExpressionAnalysis ea : deIndexToEas) {
                        if (ea.getEfvName().equals(factorValue)) {
                            series.put("pvalue", ea.getPValAdjusted());
                            series.put("expression", (ea.isUp() ? "up" : (ea.isNo() ? "no" : "dn")));
                        }
                    }

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
                } // bestGeneIndexForEfv != null end
            }
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


    private Map<String, Object> createThumbnailPlot(List<NetCDFProxy> netCDFs, String ef, String efv, int geneIndex)
            throws IOException {
        log.debug("Creating thumbnail plot... EF: {}, Top FVs: {}, Gene index: {}",
                new Object[]{ef, efv, geneIndex});

        List<Object> seriesData = new ArrayList<Object>();
        int startMark = 0;
        int endMark = 0;

        for (NetCDFProxy netCDF : netCDFs) {
            String[] assayFVs = netCDF.getFactorValues(ef);
            String[] uniqueFVs = sortUniqueFVs(assayFVs);
            float[] expressions = netCDF.getExpressionDataForDesignElementAtIndex(geneIndex);


            // iterate over each factor value (in sorted order)
            for (String factorValue : uniqueFVs) {
                // mark start position, in list of all samples, of the factor value we're after
                if (factorValue.equals(efv)) {
                    startMark = seriesData.size() + 1;
                }

                for (int assayIndex = 0; assayIndex < assayFVs.length; assayIndex++)
                    if (assayFVs[assayIndex].equals(factorValue)) {
                        float value = expressions[assayIndex];
                        seriesData.add(Arrays.<Number>asList(seriesData.size() + 1, value <= -1000000 ? null : value));
                    }

                // mark end position, in list of all samples, of the factor value we're after
                if (factorValue.equals(efv)) {
                    endMark = seriesData.size();
                }
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
                        "selection", makeMap("mode","x")
                )
        );
    }


    /**
     *
     * @param efvsToPvalues
     * @return Map: proxyId -> geneIndex corresponding to the minimum pValue, across all efvs keys in efvsToPValues
     */
    private Map<String, Integer> findGeneIndexForMinPValueAcrossAllEfvs(Map<String, TreeMap<Float, Map<String, Integer>>> efvsToPvalues) {
        Map<String, Integer> geneIndexForMinPValueAcrossAllEfvs = null;
        Float minPValueAcrossAllEfvs = 1f;
        Collection<TreeMap<Float, Map<String,Integer>>> pValsToGeneIndexesAcrossEfvs = efvsToPvalues.values();
        for (TreeMap<Float, Map<String,Integer>> pValsToGeneIndexes : pValsToGeneIndexesAcrossEfvs) {
            // Since TreeMap is sorted by its keys (pValues) in ASC order, firstKey() gives us the minimum pValue
            Float minPValueForEfv = pValsToGeneIndexes.firstKey();
            if (minPValueAcrossAllEfvs == null || minPValueAcrossAllEfvs > minPValueForEfv) {
                minPValueAcrossAllEfvs =  minPValueForEfv;
                geneIndexForMinPValueAcrossAllEfvs = pValsToGeneIndexes.get(minPValueForEfv);
            }
        }
        return geneIndexForMinPValueAcrossAllEfvs;

    }

    /**
     *
     * @param netCDF proxy from which data should be retrieved
     * @param ef experimental factor for which all efvs are to be plotted
     * @param genes for which plots are to be
     * @param genesToEfvsPvalues Map: Map: geneId -> (Map: efv -> (TreeMap: pValue -> Map: netCDF proxy Id -> geneIndex))
     * @return
     * @throws IOException
     */
    private Map<String,Object> createLargePlot(final NetCDFProxy netCDF,
                                               final String ef,
                                               final List<AtlasGene> genes,
                                               final Map<Long, Map<String, TreeMap<Float, Map<String, Integer>>>> genesToEfvsPvalues)
            throws IOException {

        log.debug("Creating big plot... EF: {}, Gene Names: [{}]",
                  new Object[]{ef, StringUtils.join(genes, " ")});

        // data for individual series
        List<Object> seriesList = new ArrayList<Object>();


        String[] assayFVs = netCDF.getFactorValues(ef);
        String[] uniqueFVs = sortUniqueFVs(assayFVs);

        // iterate over design elements axis
        // fixme: this assumes gene indices and design element indices are the same, actually we need to do a lookup
        for (AtlasGene gene : genes) {

            // Find geneIndex corresponding to the minimum pValue across all the efvs
            Map<String, Integer> proxyIdToDeIndex = findGeneIndexForMinPValueAcrossAllEfvs(genesToEfvsPvalues.get(Long.parseLong(gene.getGeneId())));
            Integer deIndex = proxyIdToDeIndex.get(netCDF.getId());
            if(deIndex == null)
                continue;

            float[] expressions = netCDF.getExpressionDataForDesignElementAtIndex(deIndex);

            // create series objects for this row of the data matrix
            List<List<Number>> seriesData = new ArrayList<List<Number>>();

            for(String factorValue : uniqueFVs) {
                for (int assayIndex = 0; assayIndex < assayFVs.length; assayIndex++)
                    if(assayFVs[assayIndex].equals(factorValue)) {
                        float value = expressions[assayIndex];
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

            // store the plot order for tihs gene
            series.put("color", seriesList.size());

            // and save this series
            seriesList.add(series);
        }

        List<Map> markings = new ArrayList<Map>();
        int position = 0;
        int flicker = 0;
        Integer[] sortedAssayOrder = new Integer[assayFVs.length];
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

        // get array design id
        final long adID = netCDF.getArrayDesignID() != -1 ? netCDF.getArrayDesignID() :
                getAtlasDatabaseDAO().getArrayDesignByAccession(netCDF.getArrayDesignAccession()).getArrayDesignID();

        final long[] deIds = netCDF.getDesignElements();
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
                        Long geneId = Long.parseLong(atlasGene.getGeneId());
                        Map<String, Integer> proxyIdToDeIndex = findGeneIndexForMinPValueAcrossAllEfvs(genesToEfvsPvalues.get(geneId));
                        String proxyId = proxyIdToDeIndex.keySet().iterator().next(); // we expect only one proxyId key in this map
                        Integer deIndex = null;
                        if (proxyId.equals(netCDF.getId())) {
                            deIndex = proxyIdToDeIndex.get(proxyId);
                        } else {
                            String errMsg = "Unexpected proxy (found id: " + proxyId + "; expected id: " + netCDF.getId() + ") found when plotting ef: " + ef;
                            log.error(errMsg);
                            throw new RuntimeException(errMsg);
                        }
                        return deIndex != null ? makeMap(
                                "deId", deIds[deIndex],
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

    private static String[] sortUniqueFVs(String[] assayFVs) {
        HashSet<String> uniqueSet = new HashSet<String>(Arrays.asList(assayFVs));
        String[] uniqueFVs = uniqueSet.toArray(new String[uniqueSet.size()]);
        Arrays.sort(uniqueFVs, new Comparator<String>() {
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

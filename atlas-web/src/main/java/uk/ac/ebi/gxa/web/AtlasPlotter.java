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

            // lookup gene names, again using SOLR index
            for(String geneIdStr : geneIdKey.split(",")) {
                AtlasSolrDAO.AtlasGeneResult gene = getAtlasSolrDAO().getGeneById(geneIdStr);
                if (gene.isFound()) {
                    genes.add(gene.getGene());
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


            Map<AtlasGene,Integer> deIndexMap = new HashMap<AtlasGene, Integer>();

            boolean[] stop = new boolean[genes.size()];
            Long[] desearch = new Long[genes.size()];

            // this is the NetCDF containing the gene we care about
            NetCDFProxy proxy = null;
            // this is a list of the indices marking the positions of design elements that correspond to the gene we're after
            for (NetCDFProxy next : proxies) {
                // loop over all de/genes in this NetCDFProxy
                long[] designElements = next.getDesignElements();
                long[] ncgenes = next.getGenes();
                for (int deIndex = 0; deIndex < designElements.length; ++deIndex) {
                    int i = 0;
                    for(AtlasGene gene : genes) {
                        if(!stop[i] && Long.valueOf(gene.getGeneId()) == ncgenes[deIndex]) {
                            proxy = next;
                            if(deIndexMap.containsKey(gene)) {
                                // okay, we got at least two DEs for the same gene. it's time to use analytics to choose one
                                Long deId = atlasDatabaseDAO.getBestDesignElementForExpressionProfile(
                                        Long.valueOf(gene.getGeneId()),
                                        Long.valueOf(experimentID),
                                        efToPlot);
                                if(deId != null) {
                                    // this (second) one is the best
                                    if(designElements[deIndex] == deId)
                                        deIndexMap.put(gene, deIndex);
                                    // not previous and not this one is the best, so put it into search queue
                                    else if(designElements[deIndexMap.get(gene)] != deId)
                                        desearch[i] = deId;
                                    // otherwise the first one was the best, it's already there
                                }
                                stop[i] = true;
                            } else {
                                deIndexMap.put(gene, deIndex);
                            }
                        } else if(desearch[i] != null) {
                            if(designElements[deIndex] == desearch[i]) {
                                deIndexMap.put(genes.get(i), deIndex);
                                desearch[i] = null;
                            }
                        }
                        ++i;
                    }
                }

                if(proxy != null) {
                    // we have found one array design with some genes, bail out as we can't handle multiple designs
                    break;
                }
            }

            // make sure proxy isn't null - if it is, break
            if (proxy != null) {
                if(!Arrays.asList(proxy.getFactors()).contains(efToPlot))
                    throw new RuntimeException("Unknown ef");

                // and build up the plot, based on plotType parameter
                if (plotType.equals("thumb")) {
                    Integer deIndex = deIndexMap.get(genes.get(0));
                    if(deIndex == null)
                        throw new RuntimeException("Can't find gene " + geneIdKey);
                    return createThumbnailPlot(proxy, efToPlot, efv, deIndex);
                }
                else if (plotType.equals("large")) {
                    return createLargePlot(proxy, efToPlot, genes, deIndexMap);
                }
                else {
                    Integer deIndex = deIndexMap.get(genes.get(0));
                    if(deIndex == null)
                        throw new RuntimeException("Can't find gene " + geneIdKey);

                    Set<String> topFVs = new HashSet<String>();

                    List<ExpressionAnalysis> atlasTuples = genes.get(0).getTopFVs(Long.valueOf(experimentID));

                    for (ExpressionAnalysis at : atlasTuples) {
                        if (at.getEfName().equals(efToPlot) && !at.getEfvName().equals("V1")) {
                            topFVs.add(at.getEfvName().toLowerCase());
                        }
                    }
                    return createBarPlot(proxy, efToPlot, topFVs, deIndex);
                }

            }
            else {
                throw new RuntimeException("Can't find genes " + geneIdKey + " in NetCDF");
            }
        }
        catch (IOException e) {
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

    private Map<String,Object> createBarPlot(NetCDFProxy netCDF, String ef, Set<String> topFVs, int geneIndex)
            throws IOException {

        log.debug("Creating plot... EF: {}, Top FVs: [{}], DE index: [{}]",
                new Object[]{ef, StringUtils.join(topFVs, ","), geneIndex});

        // get unique factor values
        String[] assayFVs = netCDF.getFactorValues(ef);
        String[] uniqueFVs = sortUniqueFVs(assayFVs);
        float[] expressions = netCDF.getExpressionDataForDesignElementAtIndex(geneIndex);

        // data for mean series
        List<List<Number>> meanSeriesData = new ArrayList<List<Number>>();

        int counter = 0;
        boolean insignificantSeries = false;
        List<Object> seriesList = new ArrayList<Object>();
        int position = 0;
        for (String factorValue : uniqueFVs) {
            // create a series for these datapoints - new series for each factor value
            List<Object> seriesData = new ArrayList<Object>();

            double meanForFV = 0;
            int meanCount = 0;

            int fvCount = 0;
            for (int assayIndex = 0; assayIndex < assayFVs.length; assayIndex++)
                if(assayFVs[assayIndex].equals(factorValue)) {
                    float value = expressions[assayIndex];
                    seriesData.add(Arrays.<Number>asList(++position, value <= -1000000 ? null : value));

                    if(value > -1000000) {
                        meanForFV += value;
                        ++meanCount;
                    }

                    ++fvCount;
                }

            for(meanForFV /= meanCount; fvCount > 0; --fvCount) {
                meanSeriesData.add(Arrays.<Number>asList(meanSeriesData.size() + 1, meanForFV <= -1000000 ? null : meanForFV));
            }

            // store the data for this factor value series
            Map<String,Object> series = makeMap(
                    "data", seriesData,
                    // and store some other standard params
                    "bars", makeMap("show", true, "align", "center", "fill", true),
                    "lines", makeMap("show", false),
                    "points", makeMap("show", false),
                    "label", factorValue,
                    "legend", makeMap("show", true)
            );

            // choose alternate series color for any insignificant factor values
            if (!topFVs.contains(factorValue.toLowerCase())) {
                series.put("color", altColors[counter % 2]);
                series.put("legend", makeMap("show", false));
                counter++;
                insignificantSeries = true;
                log.debug("Factor value: " + factorValue + " not present in topFVs (" + StringUtils.join(topFVs, ",") + "), " +
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


    private Map<String,Object> createThumbnailPlot(NetCDFProxy netCDF, String ef, String efv, int geneIndex)
            throws IOException {
        log.debug("Creating thumbnail plot... EF: {}, Top FVs: {}, Gene index: {}",
                  new Object[]{ef, efv, geneIndex});

        String[] assayFVs = netCDF.getFactorValues(ef);
        String[] uniqueFVs = sortUniqueFVs(assayFVs);
        float[] expressions = netCDF.getExpressionDataForDesignElementAtIndex(geneIndex);

        int startMark = 0;
        int endMark = 0;

        List<Object> seriesData = new ArrayList<Object>();

        // iterate over each factor value (in sorted order)
        for (String factorValue : uniqueFVs) {
            // mark start position, in list of all samples, of the factor value we're after
            if (factorValue.equals(efv)) {
                startMark = seriesData.size() + 1;
            }

            for (int assayIndex = 0; assayIndex < assayFVs.length; assayIndex++)
                if(assayFVs[assayIndex].equals(factorValue)) {
                    float value = expressions[assayIndex];
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
                        "selection", makeMap("mode","x")
                )
        );
    }


    private Map<String,Object> createLargePlot(final NetCDFProxy netCDF,
                                               final String ef,
                                               final List<AtlasGene> genes,
                                               final Map<AtlasGene,Integer> deIndexMap)
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
            Integer deIndex = deIndexMap.get(gene);
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
                        Integer deIndex = deIndexMap.get(atlasGene);
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

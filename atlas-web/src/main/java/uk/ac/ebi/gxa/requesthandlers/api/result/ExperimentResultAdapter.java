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

package uk.ac.ebi.gxa.requesthandlers.api.result;

import ae3.model.*;
import ae3.service.experiment.BestDesignElementsResult;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.JsonRestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOuts;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.XmlRestResultRenderer;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.gxa.web.AtlasPlotter;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Joiner.on;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;
import static uk.ac.ebi.gxa.utils.NumberFormatUtil.formatPValue;
import static uk.ac.ebi.gxa.utils.NumberFormatUtil.formatTValue;


/**
 * Atlas Experiment result adapter for REST serialization
 * <p/>
 * Properties from this class are handled by serializer via reflections and converted to JSOn or XML output
 *
 * @author Robert Petryszak
 * @author Tony Burdett
 * @author Andrey Zorin
 * @author Pavel Kurnosov
 */
@RestOut(xmlItemName = "result")
public class ExperimentResultAdapter {
    private final AtlasExperimentImpl experiment;
    private final ExperimentalData expData;
    private final Set<AtlasGene> genes;
    private final AtlasDAO atlasDAO;
    private final NetCDFDescriptor ncdf;
    private final BestDesignElementsResult geneResults;
    private final AtlasProperties atlasProperties;

    private Logger log = LoggerFactory.getLogger(getClass());

    public ExperimentResultAdapter(AtlasExperimentImpl experiment,
                                   BestDesignElementsResult geneResults,
                                   ExperimentalData expData,
                                   AtlasDAO atlasDAO,
                                   NetCDFDescriptor netCDFPath,
                                   AtlasProperties atlasProperties
    ) {
        this.experiment = experiment;
        this.genes = new HashSet<AtlasGene>();
        this.geneResults = geneResults;
        this.atlasDAO = atlasDAO;
        this.expData = expData;
        this.ncdf = netCDFPath;
        this.atlasProperties = atlasProperties;

        if (geneResults != null) {
            genes.addAll(geneResults.getGenes());
        }
    }

    @RestOut(name = "experimentInfo")
    public AtlasExperimentImpl getExperiment() {
        return experiment;
    }

    @RestOut(name = "experimentDesign", forProfile = ExperimentFullRestProfile.class)
    public ExperimentalData getExperimentalData() {
        return expData;
    }

    @RestOut(name = "experimentOrganisms", forProfile = ExperimentFullRestProfile.class, xmlItemName = "organism")
    public Iterable<String> getExperimentSpecies() {
        return atlasDAO.getSpeciesForExperiment(experiment.getExperiment().getId());
    }

    public static class ArrayDesignExpression {
        private final ArrayDesign arrayDesign;
        private ExperimentResultAdapter experimentResultAdapter;

        public ArrayDesignExpression(final ExperimentResultAdapter experimentResultAdapter, ArrayDesign arrayDesign) {
            this.arrayDesign = arrayDesign;
            this.experimentResultAdapter = experimentResultAdapter;
        }

        @RestOuts({
                @RestOut(forRenderer = XmlRestResultRenderer.class, name = "assayIds", asString = true),
                @RestOut(forRenderer = JsonRestResultRenderer.class, name = "assays", asString = false)
        })
        public Iterator<Integer> getAssayIds() {
            return new MappingIterator<Assay, Integer>(experimentResultAdapter.getExperimentalData().getAssays(arrayDesign).iterator()) {
                public Integer map(Assay assay) {
                    return assay.getNumber();
                }

                public String toString() {
                    return StringUtils.join(this, " ");
                }
            };
        }

        @RestOuts({
                @RestOut(forRenderer = XmlRestResultRenderer.class, asString = true),
                @RestOut(forRenderer = JsonRestResultRenderer.class, asString = false)
        })
        public static class DesignElementExpressions implements Iterable<Float> {
            private final int deIndex;
            private ArrayDesign arrayDesign;
            private ExperimentResultAdapter experimentResultAdapter;

            public DesignElementExpressions(final ArrayDesign arrayDesign, final ExperimentResultAdapter experimentResultAdapter, int deIndex) {
                this.deIndex = deIndex;
                this.arrayDesign = arrayDesign;
                this.experimentResultAdapter = experimentResultAdapter;
            }

            public Iterator<Float> iterator() {
                return new MappingIterator<Assay, Float>(experimentResultAdapter.getExperimentalData().getAssays(arrayDesign).iterator()) {
                    public Float map(Assay assay) {
                        return experimentResultAdapter.getExperimentalData().getExpression(assay, deIndex);
                    }
                };
            }

            public List<Float> list() {
                List<Float> list = new ArrayList<Float>();
                for (Float f : this) {
                    list.add(f);
                }
                return list;
            }

            public boolean isEmpty() {
                for (Float f : this)
                    if (f > -1000000.0f)
                        return false;
                return true;
            }

            @Override
            public String toString() {
                return StringUtils.join(iterator(), " ");
            }
        }

        @RestOut(xmlItemName = "designElement", xmlAttr = "id")
        public static class DesignElementExpMap extends HashMap<String, DesignElementExpressions> {
        }

        @RestOut(name = "genes", xmlItemName = "gene", xmlAttr = "id")
        public Map<String, DesignElementExpMap> getGeneExpressions() {
            Map<String, DesignElementExpMap> geneMap = new HashMap<String, DesignElementExpMap>();
            for (AtlasGene gene : experimentResultAdapter.genes) {
                int[] designElements = experimentResultAdapter.getExperimentalData().getDesignElements(arrayDesign, gene.getGeneId());
                if (designElements != null) {
                    DesignElementExpMap deMap = new DesignElementExpMap();
                    for (final int designElementId : designElements) {
                        final DesignElementExpressions designElementExpressions = new DesignElementExpressions(arrayDesign, experimentResultAdapter, designElementId);
                        if (!designElementExpressions.isEmpty())
                            deMap.put(experimentResultAdapter.getExperimentalData().getDesignElementAccession(arrayDesign, designElementId), designElementExpressions);
                    }
                    geneMap.put(gene.getGeneIdentifier(), deMap);
                }
            }
            return geneMap;
        }

        DesignElementExpMap getDesignElementExpressions(Collection<Integer> deIndexes) {
            DesignElementExpMap deMap = new DesignElementExpMap();
            for (int deIndex : deIndexes) {
                final DesignElementExpressions designElementExpressions = new DesignElementExpressions(arrayDesign, experimentResultAdapter, deIndex);
                if (!designElementExpressions.isEmpty())
                    deMap.put(Integer.toString(deIndex), designElementExpressions);
            }
            return deMap;
        }
    }

    public static class ArrayDesignStats {
        private final ArrayDesign arrayDesign;
        private ExperimentResultAdapter experimentResultAdapter;
        private Set<AtlasGene> genes;

        public ArrayDesignStats(ExperimentResultAdapter experimentResultAdapter, Set<AtlasGene> genes, ArrayDesign arrayDesign) {
            this.arrayDesign = arrayDesign;
            this.experimentResultAdapter = experimentResultAdapter;
            this.genes = genes;
        }

        @RestOut(xmlItemName = "designElement", xmlAttr = "id")
        public static class DesignElementStatMap extends HashMap<String, Object> {
        }

        @RestOut(xmlItemName = "expression")
        public static class DEExpression extends MappingIterator<EfvTree.EfEfv<ExpressionStats.Stat>, Map> {
            public DEExpression(Iterator<EfvTree.EfEfv<ExpressionStats.Stat>> fromiter) {
                super(fromiter);
            }

            public Map map(EfvTree.EfEfv<ExpressionStats.Stat> statEfEfv) {
                return makeMap("ef", statEfEfv.getEf(), "efv", statEfEfv.getEfv(), "stat", statEfEfv.getPayload());
            }
        }

        @RestOut(name = "genes", xmlItemName = "gene", xmlAttr = "id")
        public Map<String, DesignElementStatMap> getGeneExpressions() {
            Map<String, DesignElementStatMap> geneMap = new HashMap<String, DesignElementStatMap>();
            for (AtlasGene gene : genes) {
                int[] designElements = experimentResultAdapter.getExperimentalData().getDesignElements(arrayDesign, gene.getGeneId());
                if (designElements != null) {
                    DesignElementStatMap deMap = new DesignElementStatMap();
                    for (final int designElementId : designElements) {
                        List<EfvTree.EfEfv<ExpressionStats.Stat>> efefvList = experimentResultAdapter.getExperimentalData().getExpressionStats(arrayDesign, designElementId).getNameSortedList();
                        if (!efefvList.isEmpty())
                            deMap.put(experimentResultAdapter.getExperimentalData().getDesignElementAccession(arrayDesign, designElementId), new DEExpression(efefvList.iterator()));
                    }

                    geneMap.put(gene.getGeneIdentifier(), deMap);
                }
            }
            return geneMap;
        }
    }

    @RestOut(name = "geneExpressionStatistics", xmlItemName = "arrayDesign", xmlAttr = "accession", exposeEmpty = false, forProfile = ExperimentFullRestProfile.class)
    public Map<String, ArrayDesignStats> getExpressionStatistics() {
        Map<String, ArrayDesignStats> adExpMap = new HashMap<String, ArrayDesignStats>();
        if (!genes.isEmpty())
            for (ArrayDesign ad : expData.getArrayDesigns()) {
                adExpMap.put(ad.getAccession(), new ArrayDesignStats(this, genes, ad));
            }
        return adExpMap;
    }

    @RestOut(name = "geneExpressions", xmlItemName = "arrayDesign", xmlAttr = "accession", exposeEmpty = false, forProfile = ExperimentFullRestProfile.class)
    public Map<String, ArrayDesignExpression> getExpression() {

        Map<String, ArrayDesignExpression> adExpMap = new HashMap<String, ArrayDesignExpression>();
        if (!genes.isEmpty())
            for (ArrayDesign ad : expData.getArrayDesigns()) {
                adExpMap.put(ad.getAccession(), new ArrayDesignExpression(this, ad));
            }
        return adExpMap;
    }

    @RestOut(name = "genePlots", xmlItemName = "plot", xmlAttr = "experimentalFactor", exposeEmpty = false, forProfile = ExperimentPageRestProfile.class)
    public Map<String, Map<String, Map<String, Object>>> getPlots() {
        Map<String, Map<String, Map<String, Object>>> efToPlotTypeToData = Collections.emptyMap();
        String adAccession = null;
        NetCDFProxy proxy = null;
        try {
            if (ncdf == null) { // No proxy had been found for the combination of experiment id and array design id (c.f. getResults()
                return efToPlotTypeToData;
            }
            proxy = ncdf.createProxy();
            adAccession = proxy.getArrayDesignAccession();

            Map<String, ArrayDesignExpression> arrayDesignToExpressions = getExpression();
            ArrayDesignExpression ade = arrayDesignToExpressions.get(adAccession);
            if (null != ade) {
                ArrayDesignExpression.DesignElementExpMap designElementExpressions = ade.getDesignElementExpressions(geneResults.getDeIndexes());
                efToPlotTypeToData = new AtlasPlotter().getExperimentPlots(proxy, designElementExpressions, geneResults.getGenes(), geneResults.getDeIndexes());
            }
        } catch (IOException ioe) {
            log.error("Failed to generate plot data for array design: " + adAccession, ioe);
        } finally {
            closeQuietly(proxy);
        }

        return efToPlotTypeToData;
    }

    @RestOut(name = "expressionAnalyses", xmlItemName = "geneResults", exposeEmpty = true, forProfile = ExperimentPageRestProfile.class)
    public Map<String, Object> getGeneResults() {
        Map<String, Object> exprAnalysis = new HashMap<String, Object>();
        exprAnalysis.put("totalSize", geneResults.getTotalSize());
        exprAnalysis.put("items", Iterables.transform(geneResults,
                new Function<BestDesignElementsResult.Item, OutputExpressionAnalysis>() {
                    public OutputExpressionAnalysis apply(@Nonnull BestDesignElementsResult.Item atlasGeneExpressionAnalysisPair) {
                        return new OutputExpressionAnalysis(atlasGeneExpressionAnalysisPair);
                    }
                })
        );
        return exprAnalysis;
    }

    @RestOut(name = "expressionAnalysis")
    public class OutputExpressionAnalysis extends ExpressionAnalysis {
        final private AtlasGene gene;

        public OutputExpressionAnalysis(BestDesignElementsResult.Item item) {
            this.gene = item.getGene();

            this.setDesignElementID(item.getDeId());
            this.setDesignElementIndex(item.getDeIndex());
            this.setEfName(item.getEf());
            this.setEfvName(item.getEfv());
            this.setExperimentID(experiment.getExperiment().getId());
            this.setPValAdjusted(item.getPValue());
            this.setTStatistic(item.getTValue());
        }

        @RestOut(name = "ef")
        public String getEf() {
            return getEfName();
        }

        @RestOut(name = "efv")
        public String getEfv() {
            return getEfvName();
        }

        @RestOut(name = "designElementAccession")
        public String getDesignElementAccession() {
            String adAcc = getArrayDesignAccession();
            String acc = getExperimentalData().getDesignElementAccession(new ArrayDesign(adAcc), this.getDesignElementIndex());
            return acc.startsWith("Affymetrix:") ? acc.substring(1 + acc.lastIndexOf(':')) : acc;
        }

        @RestOut(name = "expression")
        public String getExpression() {
            if (isNo()) return "NON_D_E";
            if (isUp()) return "UP";
            return "DOWN";
        }

        @RestOut(name = "pval")
        public float getPval() {
            return getPValAdjusted();
        }

        @RestOut(name = "pvalPretty")
        public String getPvalPretty() {
            return formatPValue(getPValAdjusted());
        }

        @RestOut(name = "tstat")
        public float getTstat() {
            return getTStatistic();
        }

        @RestOut(name = "tstatPretty")
        public String getTstatPretty() {
            return formatTValue(getTStatistic());
        }

        @RestOut(name = "deidx")
        public Integer getDeIdx() {
            return getDesignElementIndex();
        }

        @RestOut(name = "deid")
        public long getDeId() {
            return getDesignElementID();
        }

        @RestOut(name = "experiment")
        public String getExperiment() {
            return experiment.getAccession();
        }

        @RestOut(name = "geneName")
        public String getGene() {
            return gene.getGeneName();
        }

        @RestOut(name = "geneId")
        public long getGeneId() {
            return gene.getGeneId();
        }

        @RestOut(name = "geneIdentifier")
        public String getGeneIdentifier() {
            return gene.getGeneIdentifier();
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    public static class GeneToolTipProperty {
        private String name;
        private String value;

        public GeneToolTipProperty(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @RestOut(name = "name")
        public String getName() {
            return name;
        }

        @RestOut(name = "value")
        public String getValue() {
            return value;
        }
    }

    public class GeneToolTip {
        private AtlasGene atlasGene;

        public GeneToolTip(AtlasGene atlasGene) {
            this.atlasGene = atlasGene;
        }

        @RestOut(name = "name")
        public String getName() {
            return atlasGene.getGeneName();
        }

        @RestOut(name = "identifiers")
        public String getIdentifiers() {
            return on(",").join(Collections2.transform(atlasProperties.getGeneAutocompleteNameFields(),
                    new Function<String, Object>() {
                        public Object apply(@Nonnull String geneProperty) {
                            // TODO: we apparently join Lists here. Are we sure?!
                            return atlasGene.getGeneProperties().get(geneProperty);
                        }
                    }));
        }

        @RestOut(name = "properties")
        public List<GeneToolTipProperty> getProperties() {
            List<GeneToolTipProperty> result = new ArrayList<GeneToolTipProperty>();
            for (String geneProperty : atlasProperties.getGeneTooltipFields()) {
                result.add(new GeneToolTipProperty(atlasProperties.getCuratedGeneProperties().get(geneProperty)
                        , atlasGene.getPropertyValue(geneProperty)));

            }
            return result;
        }
    }

    @RestOut(name = "geneToolTips", forProfile = ExperimentPageRestProfile.class)
    public Map<String, GeneToolTip> getGeneTooltips() {
        Map<String, GeneToolTip> tips = new HashMap<String, GeneToolTip>(genes.size());
        for (AtlasGene gene : genes) {
            tips.put("" + gene.getGeneId(), new GeneToolTip(gene));
        }
        return tips;
    }

    /**
     * @return Array Design accession in proxy in ncdf
     */
    @RestOut(name = "arrayDesign")
    public String getArrayDesignAccession() {
        if (ncdf == null) {
            return null;
        }

        NetCDFProxy proxy = null;
        try {
            proxy = ncdf.createProxy();
            return proxy.getArrayDesignAccession();
        } catch (IOException ioe) {
            log.error("Failed to generate plot data for array design do to failure to retrieve array design accession: ", ioe);
            return null;
        } finally {
            closeQuietly(proxy);
        }
    }
}

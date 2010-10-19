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
import org.apache.commons.collections.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.JsonRestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOuts;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.XmlRestResultRenderer;
import uk.ac.ebi.gxa.utils.*;

import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;
import ae3.dao.AtlasSolrDAO;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.web.AtlasPlotter;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

/**
 * Atlas Experiment result adapter for REST serialization
 *
 * Properties from this class are handled by serializer via reflections and converted to JSOn or XML output
 *
 * @author pashky
 */
@RestOut(xmlItemName = "result")
public class ExperimentResultAdapter {
    private final AtlasExperiment experiment;
    private final ExperimentalData expData;
    private final Collection<AtlasGene> genes;
    private final Collection<String> designElementIndexes;
    private final AtlasSolrDAO atlasSolrDAO;
    private final String netCDFPath;
    private final List<Pair<AtlasGene,ExpressionAnalysis>> geneResults;

    private Logger log = LoggerFactory.getLogger(getClass());

    public ExperimentResultAdapter(AtlasExperiment experiment,
                                   Collection<AtlasGene> genes,
                                   List<Pair<AtlasGene, ExpressionAnalysis>> geneResults,
                                   Collection<String> designElementIndexes,
                                   ExperimentalData expData,
                                   AtlasSolrDAO atlasSolrDAO,
                                   String netCDFPath) {
        this.experiment = experiment;
        this.genes = genes;
        this.geneResults = geneResults;
        this.designElementIndexes = designElementIndexes;
        this.atlasSolrDAO = atlasSolrDAO;
        this.expData = expData;
        this.netCDFPath = netCDFPath;
    }

    @RestOut(name="experimentInfo")
    public AtlasExperiment getExperiment() {
        return experiment;
    }

    @RestOut(name="experimentDesign", forProfile = ExperimentFullRestProfile.class)
    public ExperimentalData getExperimentalData() {
        return expData;
    }

    @RestOut(name="experimentOrganisms", forProfile = ExperimentFullRestProfile.class, xmlItemName = "organism")
    public Iterable<String> getExperimentSpecies() {
        return atlasSolrDAO.getExperimentSpecies(experiment.getId());
    }

    public class ArrayDesignExpression {
        private final ArrayDesign arrayDesign;

        public ArrayDesignExpression(ArrayDesign arrayDesign) {
            this.arrayDesign = arrayDesign;
        }

        @RestOuts({
                @RestOut(forRenderer = XmlRestResultRenderer.class, name="assayIds", asString = true),
                @RestOut(forRenderer = JsonRestResultRenderer.class, name="assays", asString = false)
        })
        public Iterator<Integer> getAssayIds() {
            return new MappingIterator<Assay,Integer>(getExperimentalData().getAssays(arrayDesign).iterator()) {
                public Integer map(Assay assay) { return assay.getNumber(); }
                public String toString() { return StringUtils.join(this, " "); }
            };
        }

        @RestOuts({
                @RestOut(forRenderer = XmlRestResultRenderer.class, asString = true),
                @RestOut(forRenderer = JsonRestResultRenderer.class, asString = false)
        })
        public class DesignElementExpressions implements Iterable<Float> {
            private final int designElementId;
            public DesignElementExpressions(int designElementId) {
                this.designElementId = designElementId;
            }

            public Iterator<Float> iterator() {
                return new MappingIterator<Assay,Float>(getExperimentalData().getAssays(arrayDesign).iterator()) {
                    public Float map(Assay assay) {
                        return getExperimentalData().getExpression(assay, designElementId);
                    }
                };
            }

            public boolean isEmpty() {
                for(Float f : this)
                    if(f > -1000000.0f)
                        return false;
                return true;
            }

            @Override
            public String toString() { return StringUtils.join(iterator(), " "); }
        }

        @RestOut(xmlItemName ="designElement", xmlAttr ="id")
        public class DesignElementExpMap extends HashMap<String,DesignElementExpressions> { }

        @RestOut(name="genes", xmlItemName ="gene", xmlAttr ="id")
        public Map<String,DesignElementExpMap> getGeneExpressions() {
            Map<String,DesignElementExpMap> geneMap = new HashMap<String, DesignElementExpMap>();
            for(AtlasGene gene : genes) {
                int[] designElements = getExperimentalData().getDesignElements(arrayDesign, Long.valueOf(gene.getGeneId()));
                if(designElements != null) {
                    DesignElementExpMap deMap = new DesignElementExpMap();
                    for(final int designElementId : designElements) {
                        final DesignElementExpressions designElementExpressions = new DesignElementExpressions(designElementId);
                        if(!designElementExpressions.isEmpty())
                            deMap.put(getExperimentalData().getDesignElementAccession(arrayDesign, designElementId), designElementExpressions);
                    }
                    geneMap.put(gene.getGeneIdentifier(), deMap);
                }
            }
            return geneMap;
        }

        @RestOut(name = "designElements", xmlItemName = "designElement", xmlAttr = "id")
        public DesignElementExpMap getDesignElementExpressions() {
            DesignElementExpMap deMap = new DesignElementExpMap();
            for (String designElementIndexStr : designElementIndexes) {
                Integer designElementIndex = Integer.parseInt(designElementIndexStr);
                final DesignElementExpressions designElementExpressions = new DesignElementExpressions(designElementIndex);
                if (!designElementExpressions.isEmpty())
                    deMap.put(designElementIndexStr, designElementExpressions);
            }
            return deMap;
        }
    }

    public class ArrayDesignStats {
        private final ArrayDesign arrayDesign;

        public ArrayDesignStats(ArrayDesign arrayDesign) {
            this.arrayDesign = arrayDesign;
        }

        @RestOut(xmlItemName ="designElement", xmlAttr ="id")
        public class DesignElementStatMap extends HashMap<String,Object> { }

        @RestOut(xmlItemName ="expression")
        public class DEExpression extends MappingIterator<EfvTree.EfEfv<ExpressionStats.Stat>,Map> {
            public DEExpression(Iterator<EfvTree.EfEfv<ExpressionStats.Stat>> fromiter) {
                super(fromiter);
            }

            public Map map(EfvTree.EfEfv<ExpressionStats.Stat> statEfEfv) {
                return makeMap("ef", statEfEfv.getEf(), "efv", statEfEfv.getEfv(), "stat", statEfEfv.getPayload());
            }
        }

        @RestOut(name="genes", xmlItemName ="gene", xmlAttr ="id")
        public Map<String,DesignElementStatMap> getGeneExpressions() {
            Map<String,DesignElementStatMap> geneMap = new HashMap<String, DesignElementStatMap>();
            for(AtlasGene gene : genes) {
                int[] designElements = getExperimentalData().getDesignElements(arrayDesign, Long.valueOf(gene.getGeneId()));
                if(designElements != null) {
                    DesignElementStatMap deMap = new DesignElementStatMap();
                    for(final int designElementId : designElements) {
                        List<EfvTree.EfEfv<ExpressionStats.Stat>> efefvList = getExperimentalData().getExpressionStats(arrayDesign, designElementId).getNameSortedList();
                        if(!efefvList.isEmpty()) 
                            deMap.put(getExperimentalData().getDesignElementAccession(arrayDesign, designElementId), new DEExpression(efefvList.iterator()));
                    }

                    geneMap.put(gene.getGeneIdentifier(), deMap);
                }
            }
            return geneMap;
        }
    }

    @RestOut(name="geneExpressionStatistics", xmlItemName ="arrayDesign", xmlAttr = "accession", exposeEmpty = false, forProfile = ExperimentFullRestProfile.class)
    public Map<String, ArrayDesignStats> getExpressionStatistics() {
        Map<String, ArrayDesignStats> adExpMap = new HashMap<String, ArrayDesignStats>();
        if(!genes.isEmpty())
            for(ArrayDesign ad : expData.getArrayDesigns()) {
                adExpMap.put(ad.getAccession(), new ArrayDesignStats(ad));
            }
        return adExpMap;
    }

    @RestOut(name="geneExpressions", xmlItemName ="arrayDesign", xmlAttr = "accession", exposeEmpty = false, forProfile = ExperimentFullRestProfile.class)
    public Map<String, ArrayDesignExpression> getExpression() {

        Map<String, ArrayDesignExpression> adExpMap = new HashMap<String, ArrayDesignExpression>();
        if(!genes.isEmpty())
            for(ArrayDesign ad : expData.getArrayDesigns()) {
                adExpMap.put(ad.getAccession(), new ArrayDesignExpression(ad));
            }
        return adExpMap;
    }

    @RestOut(name = "genePlots", xmlItemName = "arrayDesign", xmlAttr = "accession", exposeEmpty = false, forProfile = ExperimentPageRestProfile.class)
    public Map<String, Map<String, Map<String, Object>>> getPlots() {
        Map<String, Map<String, Map<String, Object>>> efToPlotTypeToData = new HashMap<String, Map<String, Map<String, Object>>>();
        Map<String, ArrayDesignExpression> arrayDesignToExpressions = getExpression();
        String adAccession = null;
        NetCDFProxy proxy = null;
        try {
            proxy = new NetCDFProxy(new File(netCDFPath));
            adAccession = proxy.getArrayDesignAccession();
            ArrayDesignExpression ade = arrayDesignToExpressions.get(adAccession);
            ArrayDesignExpression.DesignElementExpMap designElementExpressions = ade.getDesignElementExpressions();
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
            AtlasPlotter atlasPlotter = new AtlasPlotter();

            final List<String> efs = Arrays.asList(proxy.getFactors());
            for (String ef : efs) {
                    Map<String, Map<String, Object>> plotTypeToData = makeMap(
                            "large", atlasPlotter.createLargePlot(proxy, ef, bestDEIndexToGene, deIndexToExpressions),
                            "box", atlasPlotter.createBoxPlot(proxy, ef, bestDEIndexToGene, deIndexToExpressions)
                    );
                    efToPlotTypeToData.put(ef, plotTypeToData);
            }
        } catch (IOException ioe) {
            log.error("Failed to generate plot data for array design: " + adAccession, ioe);
        } finally {
            if (proxy != null) {
                proxy.close();
            }
        }
        return efToPlotTypeToData;
    }

    @RestOut(name="expressionAnalyses", xmlItemName="geneResults", exposeEmpty = true, forProfile = ExperimentPageRestProfile.class)
    public Iterable<OutputExpressionAnalysis> getGeneResults() {
        Iterable<OutputExpressionAnalysis> ea = new Iterable<OutputExpressionAnalysis>() {
            public Iterator<OutputExpressionAnalysis> iterator() {
                return new FilterIterator<Pair<AtlasGene,ExpressionAnalysis>,OutputExpressionAnalysis>(geneResults.iterator()) {
                    @Override
                    public OutputExpressionAnalysis map(Pair<AtlasGene, ExpressionAnalysis> atlasGeneExpressionAnalysisPair) {
                        return new OutputExpressionAnalysis(atlasGeneExpressionAnalysisPair);
                    }
                };
            }
        };

        return ea;
    }

    @RestOut(name="expressionAnalysis")
    public class OutputExpressionAnalysis extends ExpressionAnalysis {
        final private AtlasGene gene;

        public OutputExpressionAnalysis(Pair<AtlasGene, ExpressionAnalysis> eaPair) {
            this.gene = eaPair.getFirst();

            ExpressionAnalysis ea = eaPair.getSecond();
            this.setDesignElementID(ea.getDesignElementID());
            this.setDesignElementIndex(ea.getDesignElementIndex());
            this.setEfName(ea.getEfName());
            this.setEfvName(ea.getEfvName());
            this.setExperimentID(experiment.getId());
            this.setPValAdjusted(ea.getPValAdjusted());
            this.setTStatistic(ea.getTStatistic());
            this.setEfoAccessions(ea.getEfoAccessions());
            this.setEfId(ea.getEfId());
            this.setEfvId(ea.getEfvId());
            this.setProxyId(ea.getProxyId());
        }

        @RestOut(name="ef")
        public String getEf() {
            return getEfName();
        }

        @RestOut(name="efv")
        public String getEfv() {
            return getEfvName();
        }

        @RestOut(name="designElementAccession")
        public String getDesignElementAccession() {
            Set<ArrayDesign> ads = getExperimentalData().getArrayDesigns();
            for (ArrayDesign ad : ads) {
                String acc = getExperimentalData().getDesignElementAccession(ad, this.getDesignElementIndex());
                if(acc != null)
                    return acc.startsWith("Affymetrix:") ? acc.substring(1 + acc.lastIndexOf(':')) : acc;
            }

            return "Unknown";
        }

        @RestOut(name="expression")
        public String getExpression() {
            if(isNo()) return "NON_D_E";
            if(isUp()) return "UP";
            return "DOWN";
        }

        @RestOut(name="pval")
        public float getPval() {
            return getPValAdjusted();
        }

        @RestOut(name="pvalPretty")
        public String getPvalPretty() {
            return NumberFormatUtil.prettyFloatFormat(getPValAdjusted());
        }
        
        @RestOut(name="tstat")
        public float getTstat() {
            return getTStatistic();
        }

        @RestOut(name="deidx")
        public Integer getDeIdx() {
            return getDesignElementIndex();
        }

        @RestOut(name="deid")
        public long getDeId() {
            return getDesignElementID();
        }

        @RestOut(name="experiment")
        public String getExperiment() {
            return experiment.getAccession();
        }

        @RestOut(name="geneName")
        public String getGene() {
            return gene.getGeneName();
        }

        @RestOut(name="geneId")
        public String getGeneId() {
            return gene.getGeneId();
        }

        @RestOut(name="geneIdentifier")
        public String getGeneIdentifier() {
            return gene.getGeneIdentifier();
        }
    }
}

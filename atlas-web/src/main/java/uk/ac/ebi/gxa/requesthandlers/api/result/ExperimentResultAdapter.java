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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.properties.AtlasProperties;
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
    private final Set<AtlasGene> genes;
    // Since we plot design elements rather than genes, the same gene may appear in genesToPlot more then once
    private final List<AtlasGene> genesToPlot;
    private final Collection<String> designElementIndexes;
    private final AtlasSolrDAO atlasSolrDAO;
    private final String netCDFPath;
    private final List<Pair<AtlasGene,ExpressionAnalysis>> geneResults;
    private final AtlasProperties atlasProperties;

    private Logger log = LoggerFactory.getLogger(getClass());

    public ExperimentResultAdapter(AtlasExperiment experiment,
                                   List<AtlasGene> genesToPlot,
                                   List<Pair<AtlasGene, ExpressionAnalysis>> geneResults,
                                   Collection<String> designElementIndexes,
                                   ExperimentalData expData,
                                   AtlasSolrDAO atlasSolrDAO,
                                   String netCDFPath,
                                   AtlasProperties atlasProperties) {
        this.experiment = experiment;
        this.genes = new HashSet<AtlasGene>(genesToPlot);
        this.genesToPlot = genesToPlot;
        this.geneResults = geneResults;
        this.designElementIndexes = designElementIndexes;
        this.atlasSolrDAO = atlasSolrDAO;
        this.expData = expData;
        this.netCDFPath = netCDFPath;
        this.atlasProperties = atlasProperties;
    }

    @RestOut(name="experimentInfo")
    public AtlasExperiment getExperiment() {
        return experiment;
    }

    @RestOut(name="experimentDesign", forProfile = ExperimentFullRestProfile.class)
    public ExperimentalData getExperimentalData() {
        return expData;
    }

    @RestOut(name = "sampleCharacteristicValuesForPlot", forProfile = ExperimentPageHeaderRestProfile.class)
    public Collection<SampleCharacteristicsCompactData> getSampleCharacteristicValuesForPlot() {
        String adAccession = getArrayDesignAccession();
        if (adAccession != null) {
            return expData.getSCVsForPlot(adAccession);
        }
        // Return an empty result set if array design accession could not be retrieved (the most likely cause, other than
        // an IOException, is that no proxy could be found for the combination of experiment id and array design id provided
        // in the API call.
        return new ArrayList<SampleCharacteristicsCompactData>();
    }

    @RestOut(name = "experimentalFactorValuesForPlot", forProfile = ExperimentPageHeaderRestProfile.class)
    public Collection<ExperimentalFactorsCompactData> getExperimentalFactorValuesForPlot() {
        String adAccession = getArrayDesignAccession();
        if (adAccession != null) {
            return expData.getEFVsForPlot(adAccession);
        }
        // Return an empty result set if array design accession could not be retrieved (the most likely cause, other than
        // an IOException, is that no proxy could be found for the combination of experiment id and array design id provided
        // in the API call.
        return new ArrayList<ExperimentalFactorsCompactData>();
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

            public List<Float> list() {
                List<Float> list = new ArrayList<Float>();
                for(Iterator<Float> iter = iterator(); iter.hasNext();) {
                    list.add(iter.next());
                }
                return list;
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

    @RestOut(name = "genePlots", xmlItemName = "plot", xmlAttr = "experimentalFactor", exposeEmpty = false, forProfile = ExperimentPageRestProfile.class)
    public Map<String, Map<String, Map<String, Object>>> getPlots() {
        Map<String, Map<String, Map<String, Object>>> efToPlotTypeToData = Collections.emptyMap();
        String adAccession = null;
        NetCDFProxy proxy = null;
        try {
            if (netCDFPath == null) { // No proxy had been found for the combination of experiment id and array design id (c.f. getResults() 
               return efToPlotTypeToData;
            }
            proxy = new NetCDFProxy(new File(netCDFPath));
            adAccession = proxy.getArrayDesignAccession();

            Map<String, ArrayDesignExpression> arrayDesignToExpressions = getExpression();
            ArrayDesignExpression ade = arrayDesignToExpressions.get(adAccession);
            if(null != ade) {
                ArrayDesignExpression.DesignElementExpMap designElementExpressions = ade.getDesignElementExpressions();
                efToPlotTypeToData = new AtlasPlotter().getExperimentPlots(proxy, designElementExpressions, genesToPlot, designElementIndexes);
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

        @RestOut(name="tstatPretty")
        public String getTstatPretty() {
            return String.format("%.3f%n",getTStatistic());
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
    public class GeneToolTipProperty{
        private String name;
        private String value;
        public GeneToolTipProperty(String name,String value){
            this.name= name;
            this.value= value;
        }
        @RestOut(name="name")
        public String getName(){
            return name;
        }
        @RestOut(name="value")
        public String getValue(){
            return value;
        }
    }
    public class GeneToolTip{
        private AtlasGene atlasGene;
        public GeneToolTip(AtlasGene atlasGene){
            this.atlasGene = atlasGene; 
        }
        @RestOut(name="name")
        public String getName(){
            return atlasGene.getGeneName();
        }
        @RestOut(name="identifiers")
        public String getIdentifiers(){
            String result = "";
            for(String geneProperty : atlasProperties.getGeneAutocompleteNameFields()){
                result+=(("".equals(result)?"":",")+atlasGene.getGeneProperties().get(geneProperty));
            }
            return result;
        }
        @RestOut(name="properties")
        public List<GeneToolTipProperty> getProperties(){
            List<GeneToolTipProperty> result = new ArrayList<GeneToolTipProperty>();
            for(String geneProperty : atlasProperties.getGeneTooltipFields()){
                result.add(new GeneToolTipProperty(atlasProperties.getCuratedGeneProperties().get(geneProperty)
                                                  ,atlasGene.getPropertyValue(geneProperty)));

            }
            return result;
        }
    }
    @RestOut(name="geneToolTips", forProfile=ExperimentPageRestProfile.class)
    public Map<String,GeneToolTip> getGeneTooltips() {
       Map<String,GeneToolTip> tips = new HashMap<String,GeneToolTip>(genes.size());
       for (AtlasGene gene : genes) {
           tips.put(gene.getGeneId(), new GeneToolTip(gene));
       }
       return tips;
    }
    /**
     * @return Array Design accession in proxy in netCDFPath
     */
    private String getArrayDesignAccession() {
        String adAccession = null;

        if (netCDFPath != null) {
            NetCDFProxy proxy = null;
            try {
                proxy = new NetCDFProxy(new File(netCDFPath));
                adAccession = proxy.getArrayDesignAccession();

            } catch (IOException ioe) {
                log.error("Failed to generate plot data for array design do to failure to retrieve array design accession: ", ioe);
            } finally {
                if (proxy != null) {
                    proxy.close();
                }
            }
        }

        return adAccession;
    }
}

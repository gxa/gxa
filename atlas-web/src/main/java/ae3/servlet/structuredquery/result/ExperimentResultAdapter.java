package ae3.servlet.structuredquery.result;

import ae3.model.*;
import ae3.restresult.JsonRestResultRenderer;
import ae3.restresult.RestOut;
import ae3.restresult.RestOuts;
import ae3.restresult.XmlRestResultRenderer;
import ae3.service.structuredquery.EfvTree;
import ae3.util.MappingIterator;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * @author pashky
 */
public class ExperimentResultAdapter {
    private final AtlasExperiment experiment;
    private final Collection<AtlasGene> genes;

    public ExperimentResultAdapter(AtlasExperiment experiment, Collection<AtlasGene> genes) {
        this.experiment = experiment;
        this.genes = genes;
    }

    @RestOut(name="experimentInfo")
    public AtlasExperiment getExperiment() {
        return experiment;
    }

    @RestOut(name="experimentDesign")
    public ExperimentalData getExperimentalData() {
        return experiment.getExperimentalData();
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
        public class DesignElementExpressions implements Iterable<Double> {
            private final int designElementId;
            public DesignElementExpressions(int designElementId) {
                this.designElementId = designElementId;
            }

            public Iterator<Double> iterator() {
                return new MappingIterator<Assay,Double>(getExperimentalData().getAssays(arrayDesign).iterator()) {
                    public Double map(Assay assay) {
                        return getExperimentalData().getExpression(assay, designElementId);
                    }
                };
            }

            public boolean isEmpty() {
                for(Double d : this)
                    if(d > -1000000.0d)
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
                int[] designElements = getExperimentalData().getDesignElements(arrayDesign, Integer.valueOf(gene.getGeneId()));
                if(designElements != null) {
                    DesignElementExpMap deMap = new DesignElementExpMap();
                    for(final int designElementId : designElements) {
                        final DesignElementExpressions designElementExpressions = new DesignElementExpressions(designElementId);
                        if(!designElementExpressions.isEmpty())
                            deMap.put(String.valueOf(designElementId), designElementExpressions);
                    }
                    geneMap.put(gene.getGeneIdentifier(), deMap);
                }
            }
            return geneMap;
        }
    }

    public class ArrayDesignStats {
        private final ArrayDesign arrayDesign;

        public ArrayDesignStats(ArrayDesign arrayDesign) {
            this.arrayDesign = arrayDesign;
        }

        @RestOut(xmlItemName ="designElement", xmlAttr ="id")
        public class DesignElementStatMap extends HashMap<String,Object> { }

        @RestOut(name="genes", xmlItemName ="gene", xmlAttr ="id")
        public Map<String,DesignElementStatMap> getGeneExpressions() {
            Map<String,DesignElementStatMap> geneMap = new HashMap<String, DesignElementStatMap>();
            for(AtlasGene gene : genes) {
                int[] designElements = getExperimentalData().getDesignElements(arrayDesign, Integer.valueOf(gene.getGeneId()));
                if(designElements != null) {
                    DesignElementStatMap deMap = new DesignElementStatMap();
                    for(final int designElementId : designElements) {
                        List<EfvTree.EfEfv<ExpressionStats.Stat>> efefvList = getExperimentalData().getExpressionStats(arrayDesign, designElementId).getNameSortedList();
                        if(!efefvList.isEmpty()) 
                            deMap.put(String.valueOf(designElementId), efefvList);
                    }

                    geneMap.put(gene.getGeneIdentifier(), deMap);
                }
            }
            return geneMap;
        }

    }

    @RestOut(name="geneExpressionStatistics", xmlItemName ="arrayDesign", xmlAttr = "accession", exposeEmpty = false)
    public Map<String, ArrayDesignStats> getExpressionStatistics() {
        Map<String, ArrayDesignStats> adExpMap = new HashMap<String, ArrayDesignStats>();
        if(!genes.isEmpty())
            for(ArrayDesign ad : experiment.getExperimentalData().getArrayDesigns()) {
                adExpMap.put(ad.getAccession(), new ArrayDesignStats(ad));
            }
        return adExpMap;
    }

    @RestOut(name="geneExpressions", xmlItemName ="arrayDesign", xmlAttr = "accession", exposeEmpty = false)
    public Map<String, ArrayDesignExpression> getExpression() {
        Map<String, ArrayDesignExpression> adExpMap = new HashMap<String, ArrayDesignExpression>();
        if(!genes.isEmpty())
            for(ArrayDesign ad : experiment.getExperimentalData().getArrayDesigns()) {
                adExpMap.put(ad.getAccession(), new ArrayDesignExpression(ad));
            }
        return adExpMap;
    }
}

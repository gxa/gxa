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
import uk.ac.ebi.gxa.requesthandlers.base.restutil.JsonRestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOuts;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.XmlRestResultRenderer;
import ae3.service.structuredquery.EfvTree;
import uk.ac.ebi.gxa.utils.MappingIterator;
import ae3.dao.AtlasSolrDAO;

import java.util.*;

import org.apache.commons.lang.StringUtils;

/**
 * Atlas Experiment result adapter for REST serialization
 * @author pashky
 */
@RestOut(xmlItemName = "result")
public class ExperimentResultAdapter {
    private final AtlasExperiment experiment;
    private final ExperimentalData expData;
    private final Collection<AtlasGene> genes;
    private final AtlasSolrDAO atlasSolrDAO;

    public ExperimentResultAdapter(AtlasExperiment experiment, Collection<AtlasGene> genes, ExperimentalData expData, AtlasSolrDAO atlasSolrDAO) {
        this.experiment = experiment;
        this.genes = genes;
        this.atlasSolrDAO = atlasSolrDAO;
        this.expData = expData;
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
                int[] designElements = getExperimentalData().getDesignElements(arrayDesign, Long.valueOf(gene.getGeneId()));
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
}

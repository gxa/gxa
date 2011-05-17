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
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.JsonRestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOuts;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.XmlRestResultRenderer;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.MappingIterator;

import java.util.*;

import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;


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
    private final AtlasExperiment experiment;
    private final ExperimentalData expData;
    private final Set<AtlasGene> genes = new HashSet<AtlasGene>();

    public ExperimentResultAdapter(AtlasExperiment experiment,
                                   Collection<AtlasGene> genes,
                                   ExperimentalData expData
    ) {
        this.experiment = experiment;
        this.expData = expData;
        this.genes.addAll(genes);
    }

    @RestOut(name = "experimentInfo")
    public AtlasExperiment getExperiment() {
        return experiment;
    }

    @RestOut(name = "experimentDesign", forProfile = ExperimentFullRestProfile.class)
    public ExperimentalData getExperimentalData() {
        return expData;
    }

    @RestOut(name = "experimentOrganisms", forProfile = ExperimentFullRestProfile.class, xmlItemName = "organism")
    public Iterable<String> getExperimentSpecies() {
        return experiment.getExperiment().getSpecies();
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
}

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

import ae3.model.ListResultRowExperiment;
import ae3.service.AtlasStatisticsQueryService;
import ae3.service.structuredquery.*;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.Model;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.JoinIterator;
import uk.ac.ebi.microarray.atlas.model.Expression;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

/**
 * Gene search "heatmap" REST API result view.
 * <p/>
 * Properties from this class are handled by serializer via reflections and converted to JSOn or XML output
 *
 * @author pashky
 */
public class HeatmapResultAdapter implements ApiQueryResults<HeatmapResultAdapter.ResultRow> {
    private final AtlasStructuredQueryResult r;
    private final Model atlasModel;
    private final AtlasProperties atlasProperties;
    private final Collection<String> geneIgnoreProp;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;
    private Map<String,Experiment> experimentsCache = new HashMap<String,Experiment>();

    public HeatmapResultAdapter(AtlasStructuredQueryResult r, Model atlasModel, AtlasProperties atlasProperties, AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.r = r;
        this.atlasModel = atlasModel;
        this.atlasProperties = atlasProperties;
        this.geneIgnoreProp = new HashSet<String>(atlasProperties.getGeneApiIgnoreFields());
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public long getTotalResults() {
        return r.getTotal();
    }

    public long getNumberOfResults() {
        return r.getSize();
    }

    public long getStartingFrom() {
        return r.getStart();
    }

    @RestOut(xmlItemName = "result")
    public class ResultRow {
        private final StructuredResultRow row;

        public abstract class Expression {
            UpdownCounter counter;

            public int getUpExperiments() {
                return counter.getUps();
            }

            public int getDownExperiments() {
                return counter.getDowns();
            }

            public int getNonDEExperiments() {
                return counter.getNones();
            }

            public double getUpPvalue() {
                return counter.getMpvUp();
            }

            public double getDownPvalue() {
                return counter.getMpvDn();
            }

            public Iterator<ListResultRowExperiment> getExperiments() {
                return Iterators.filter(
                        Iterators.transform(
                                Iterators.filter(expiter(), Predicates.<Object>notNull()),
                                new Function<ExperimentInfo, ListResultRowExperiment>() {
                                    public ListResultRowExperiment apply(@Nonnull ExperimentInfo e) {
                                        Experiment exp = getExperiment(e.getAccession());
                                        if (exp == null) return null;
                                        return new ListResultRowExperiment(e.getExperimentId(), exp.getAccession(),
                                                exp.getDescription(), e.getpValTStatRank().getPValue(),
                                                toExpression(e.getpValTStatRank()));
                                    }
                                }),
                        Predicates.<ListResultRowExperiment>notNull());
            }

            abstract Iterator<ExperimentInfo> expiter();
        }

        public class EfvExp extends ResultRow.Expression {
            private EfvTree.EfEfv<? extends ColumnInfo> efefv;

            public EfvExp(EfvTree.EfEfv<? extends ColumnInfo> efefv) {
                this.efefv = efefv;
                this.counter = row.getCounters().get(efefv.getPayload().getPosition());
            }

            public String getEf() {
                return efefv.getEf();
            }

            public String getEfv() {
                return efefv.getEfv();
            }

            Iterator<ExperimentInfo> expiter() {
                EfvAttribute attr = new EfvAttribute(efefv.getEf(), efefv.getEfv(), StatisticsType.UP_DOWN);
                return atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(row.getGene().getGeneId(), attr, -1, -1).iterator();
            }
        }

        public class EfoExp extends ResultRow.Expression {
            private EfoTree.EfoItem<? extends ColumnInfo> efoItem;

            public EfoExp(EfoTree.EfoItem<? extends ColumnInfo> efoItem) {
                this.efoItem = efoItem;
                this.counter = row.getCounters().get(efoItem.getPayload().getPosition());
            }

            public String getEfoTerm() {
                return efoItem.getTerm();
            }

            public String getEfoId() {
                return efoItem.getId();
            }

            Iterator<ExperimentInfo> expiter() {
                Attribute attr = new EfoAttribute(efoItem.getId(), StatisticsType.UP_DOWN);
                return atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(row.getGene().getGeneId(), attr, -1, -1).iterator();
            }
        }

        public ResultRow(StructuredResultRow row) {
            this.row = row;
        }

        public Map getGene() {
            Map<String, Object> gene = makeMap(
                    "id", row.getGene().getGeneIdentifier(),
                    "name", row.getGene().getGeneName(),
                    "organism", row.getGene().getGeneSpecies(),
                    "orthologs", row.getGene().getOrthologs());
            for (Map.Entry<String, Collection<String>> prop : row.getGene().getGeneProperties().entrySet()) {
                if (!geneIgnoreProp.contains(prop.getKey()) && !prop.getValue().isEmpty()) {
                    String field = atlasProperties.getGeneApiFieldName(prop.getKey());
                    gene.put(field, field.endsWith("s") ? prop.getValue() : prop.getValue().iterator().next());
                }
            }
            return gene;
        }

        public Iterator<ResultRow.Expression> getExpressions() {
            return new JoinIterator<
                    EfvTree.EfEfv<ColumnInfo>,
                    EfoTree.EfoItem<ColumnInfo>,
                    ResultRow.Expression
                    >(r.getResultEfvs().getNameSortedList().iterator(),
                    r.getResultEfos().getExplicitList().iterator()) {

                public Expression map1(EfvTree.EfEfv<ColumnInfo> from) {
                    UpdownCounter cnt = row.getCounters().get(from.getPayload().getPosition());
                    if (cnt.isZero()) {
                        return null;
                    }
                    return new ResultRow.EfvExp(from);
                }

                public Expression map2(EfoTree.EfoItem<ColumnInfo> from) {
                    if (row.getCounters().get(from.getPayload().getPosition()).isZero()) {
                        return null;
                    }
                    return new ResultRow.EfoExp(from);
                }
            };
        }
    }

    public Iterator<ResultRow> getResults() {
        return Iterators.transform(r.getResults().iterator(),
                new Function<StructuredResultRow, ResultRow>() {
                    public ResultRow apply(@Nullable StructuredResultRow input) {
                        return new ResultRow(input);
                    }
                });
    }

    static Expression toExpression(PvalTstatRank pvalTstatRank) {
        if (ExpressionAnalysis.isNo(pvalTstatRank.getPValue(), pvalTstatRank.getTStatRank()))
            return Expression.NONDE;
        if (ExpressionAnalysis.isUp(pvalTstatRank.getPValue(), pvalTstatRank.getTStatRank()))
            return Expression.UP;
        return Expression.DOWN;
    }

    /**
     *
     * @param experimentId
     * @return Experiment corresponding to experimentId; if not already in cache, get it from Oracle and add it to the cache
     */
    private Experiment getExperiment(String accession) {
        if (!experimentsCache.containsKey(accession)) {
            experimentsCache.put(accession, atlasModel.getExperimentByAccession(accession));
        }
        return experimentsCache.get(accession);
    }
}

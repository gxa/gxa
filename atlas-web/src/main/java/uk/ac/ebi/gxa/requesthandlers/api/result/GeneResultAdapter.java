package uk.ac.ebi.gxa.requesthandlers.api.result;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ListResultRowExperiment;
import ae3.service.structuredquery.EfoTree;
import ae3.service.structuredquery.UpdownCounter;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.gxa.utils.FilterIterator;
import uk.ac.ebi.gxa.utils.JoinIterator;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.util.*;

import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

/**
 * Adaptor for a single gene
 *
 */
public class GeneResultAdapter implements ApiQueryResults<GeneResultAdapter.GeneResult>{
    final private AtlasProperties atlasProperties;
    final private AtlasGene gene;
    final private Set<String> geneIgnoreProp;
    final private Efo efo;
    private AtlasSolrDAO atlasSolrDAO;

    public GeneResultAdapter(final AtlasGene gene, final AtlasProperties atlasProperties, final Efo efo, final AtlasSolrDAO atlasSolrDAO) {
        this.atlasProperties = atlasProperties;
        this.gene = gene;
        this.geneIgnoreProp = new HashSet<String>(atlasProperties.getGeneApiIgnoreFields());
	this.atlasSolrDAO = atlasSolrDAO;
	this.efo = efo;
    }

    public long getTotalResults() {
        return 1;
    }

    public long getNumberOfResults() {
        return 1;
    }

    public long getStartingFrom() {
        return 0;
    }

    public Iterator<GeneResult> getResults() {
        return Collections.singletonList(new GeneResult(gene)).iterator();
    }

    @RestOut(name = "result")
    public class GeneResult {
        final private AtlasGene gene_;

        public GeneResult(final AtlasGene gene) {
            this.gene_ = gene;
        }

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
                return new FilterIterator<ExpressionAnalysis, ListResultRowExperiment>(expiter()) {
                    public ListResultRowExperiment map(ExpressionAnalysis e) {
                        AtlasExperiment aexp = atlasSolrDAO.getExperimentById(e.getExperimentID());
                        if (aexp == null) {
                            return null;
                        }
                        return new ListResultRowExperiment(e.getExperimentID(), aexp.getAccession(),
                                                           aexp.getDescription(), e.getPValAdjusted(),
                                                           e.isNo() ? ae3.model.Expression.NONDE :
                                                                   (e.isUp() ? ae3.model.Expression.UP : ae3.model.Expression.DOWN));
                    }
                };
            }

            abstract Iterator<ExpressionAnalysis> expiter();
        }

        public class EfvExp extends Expression {
            private EfvTree.EfEfv<UpdownCounter> efefv;

            public EfvExp(EfvTree.EfEfv<UpdownCounter> efefv) {
                this.efefv = efefv;
                this.counter = efefv.getPayload();
            }

            public String getEf() {
                return efefv.getEf();
            }

            public String getEfv() {
                return efefv.getEfv();
            }

            Iterator<ExpressionAnalysis> expiter() {
                return gene_.getExpressionAnalyticsTable().findByEfEfv(efefv.getEf(), efefv.getEfv()).iterator();
            }
        }

        public class EfoExp extends Expression {
            private EfoTree.EfoItem<UpdownCounter> efoItem;

            public EfoExp(EfoTree.EfoItem<UpdownCounter> efoItem) {
                this.efoItem = efoItem;
                this.counter = efoItem.getPayload();
            }

            public String getEfoTerm() {
                return efoItem.getTerm();
            }

            public String getEfoId() {
                return efoItem.getId();
            }

            Iterator<ExpressionAnalysis> expiter() {
                return gene_.getExpressionAnalyticsTable()
                        .findByEfoSet(efo.getTermAndAllChildrenIds(efoItem.getId())).iterator();
            }
        }

        public Iterator<Expression> getExpressions() {
            return new JoinIterator<
                                EfvTree.EfEfv<UpdownCounter>,
                                EfoTree.EfoItem<UpdownCounter>,
                                Expression
                                >(gene_.getHeatMap(atlasProperties.getGeneHeatmapIgnoredEfs()).getValueSortedList().iterator(),
                                  gene_.getEfoTree(null, efo).getValueOrderedList().iterator()) {

                public Expression map1(EfvTree.EfEfv<UpdownCounter> from) {
                    return new EfvExp(from);
                }

                public Expression map2(EfoTree.EfoItem<UpdownCounter> from) {
                    return new EfoExp(from);
                }
            };
        }

        public Map getGene() {
            Map<String, Object> gene = makeMap(
                    "id", gene_.getGeneIdentifier(),
                    "name", gene_.getGeneName(),
                    "organism", gene_.getGeneSpecies(),
                    "orthologs", gene_.getOrthologs());
            for(Map.Entry<String, Collection<String>> prop : gene_.getGeneProperties().entrySet()) {
                if(!geneIgnoreProp.contains(prop.getKey()) && !prop.getValue().isEmpty()) {
                    String field = atlasProperties.getGeneApiFieldName(prop.getKey());
                    gene.put(field, field.endsWith("s") ? prop.getValue() : prop.getValue().iterator().next());
                }
            }

            return gene;
        }
    }
}

package ae3.servlet.structuredquery.result;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ListResultRowExperiment;
import ae3.service.structuredquery.*;
import ae3.util.FilterIterator;
import ae3.util.JoinIterator;
import ae3.util.MappingIterator;
import uk.ac.ebi.gxa.index.Experiment;
import uk.ac.ebi.gxa.efo.Efo;

import java.util.Iterator;

/**
 * @author pashky
 */
public class HeatmapResultAdapter {
    private final AtlasStructuredQueryResult r;
    private final AtlasDao dao;
    private final Efo efo;

    public HeatmapResultAdapter(AtlasStructuredQueryResult r, AtlasDao dao, Efo efo) {
        this.r = r;
        this.dao = dao;
        this.efo = efo;
    }

    public long getTotalResultGenes() {
        return r.getTotal();
    }

    public long getNumberOfResultGenes() {
        return r.getSize();
    }

    public long getStartingFrom() {
        return r.getStart();
    }

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

            public double getUpPvalue() {
                return counter.getMpvUp();
            }

            public double getDownPvalue() {
                return counter.getMpvDn();
            }

            public Iterator<ListResultRowExperiment> getExperiments() {
                return new FilterIterator<Experiment, ListResultRowExperiment>(expiter()) {
                    public ListResultRowExperiment map(Experiment e) {
                        AtlasExperiment aexp = dao.getExperimentById(e.getId());
                        if (aexp == null) {
                            return null;
                        }
                        return new ListResultRowExperiment(e.getId(), aexp.getAccession(),
                                                           aexp.getDescription(), e.getPvalue(),
                                                           e.getExpression());
                    }
                };
            }

            abstract Iterator<Experiment> expiter();
        }

        public class EfvExp extends ResultRow.Expression {
            private EfvTree.EfEfv<Integer> efefv;

            public EfvExp(EfvTree.EfEfv<Integer> efefv) {
                this.efefv = efefv;
                this.counter = row.getCounters().get(efefv.getPayload());
            }

            public String getEf() {
                return efefv.getEf();
            }

            public String getEfv() {
                return efefv.getEfv();
            }

            Iterator<Experiment> expiter() {
                return row.getGene().getExperimentsTable().findByEfEfv(efefv.getEf(), efefv.getEfv()).iterator();
            }
        }

        public class EfoExp extends ResultRow.Expression {
            private EfoTree.EfoItem<Integer> efoItem;

            public EfoExp(EfoTree.EfoItem<Integer> efoItem) {
                this.efoItem = efoItem;
                this.counter = row.getCounters().get(efoItem.getPayload());
            }

            public String getEfoTerm() {
                return efoItem.getTerm();
            }

            public String getEfoId() {
                return efoItem.getId();
            }

            Iterator<Experiment> expiter() {
                return row.getGene().getExperimentsTable()
                        .findByEfoSet(efo.getTermAndAllChildrenIds(efoItem.getId())).iterator();
            }
        }

        public ResultRow(StructuredResultRow row) {
            this.row = row;
        }

        public AtlasGene getGene() {
            return row.getGene();
        }

        public Iterator<ResultRow.Expression> getExpressions() {
            return new JoinIterator<
                    EfvTree.EfEfv<Integer>,
                    EfoTree.EfoItem<Integer>,
                    ResultRow.Expression
                    >(r.getResultEfvs().getNameSortedList().iterator(),
                      r.getResultEfos().getExplicitList().iterator()) {

                public Expression map1(EfvTree.EfEfv<Integer> from) {
                    if (row.getCounters().get(from.getPayload()).isZero()) {
                        return null;
                    }
                    return new ResultRow.EfvExp(from);
                }

                public Expression map2(EfoTree.EfoItem<Integer> from) {
                    if (row.getCounters().get(from.getPayload()).isZero()) {
                        return null;
                    }
                    return new ResultRow.EfoExp(from);
                }
            };
        }
    }

    public Iterator<ResultRow> getResults() {
        return new MappingIterator<StructuredResultRow, ResultRow>(r.getResults().iterator()) {
            public ResultRow map(StructuredResultRow srr) {
                return new ResultRow(srr);
            }
        };
    }
}

package ae3.servlet.structuredquery;

import javax.servlet.http.HttpServletRequest;

import ae3.service.structuredquery.*;
import ae3.service.ArrayExpressSearchService;
import ae3.restresult.RestOut;
import ae3.dao.AtlasDao;
import ae3.model.ListViewRestProfile;
import ae3.model.GeneViewRestProfile;
import ae3.model.AtlasGene;

import java.util.*;

/**
 * @author pashky
 */
public class ApiStructuredQueryServlet extends RestServlet {

    public static class HeatmapResultAdapter {
        private final AtlasStructuredQueryResult r;

        public HeatmapResultAdapter(AtlasStructuredQueryResult r) {
            this.r = r;
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

            public class Expression {
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
            }

            public class EfvExp extends Expression {
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
            }

            public class EfoExp extends Expression {
                private EfoTree.EfoItem<Integer> efo;

                public EfoExp(EfoTree.EfoItem<Integer> efo) {
                    this.efo = efo;
                    this.counter = row.getCounters().get(efo.getPayload());
                }

                public String getEfoTerm() {
                    return efo.getTerm();
                }

                public String getEfoId() {
                    return efo.getId();
                }

            }

            public ResultRow(StructuredResultRow row) {
                this.row = row;
            }

            public AtlasGene getGene() {
                return row.getGene();
            }

            public Iterator<Expression> getExpressions() {
                return new Iterator<Expression>() {
                    Iterator<EfvTree.EfEfv<Integer>> efvi = r.getResultEfvs().getNameSortedList().iterator();
                    Iterator<EfoTree.EfoItem<Integer>> efoi = r.getResultEfos().getExplicitList().iterator();

                    Expression val;
                    EfoTree.EfoItem<Integer> efoval;

                    { skip(); }

                    public boolean hasNext() {
                        return val != null || efvi.hasNext() || efoi.hasNext();
                    }

                    public Expression next() {
                        Expression e = val;
                        skip();
                        return e;
                    }

                    private void skip() {
                        val = null;
                        while(efvi.hasNext()) {
                            EfvTree.EfEfv<Integer> v = efvi.next();
                            if(!row.getCounters().get(v.getPayload()).isZero()) {
                                val = new EfvExp(v);
                                return;
                            }
                        }
                        while(efoi.hasNext()) {
                            EfoTree.EfoItem<Integer> v = efoi.next();
                            if(!row.getCounters().get(v.getPayload()).isZero()) {
                                val = new EfoExp(v);
                                return;
                            }
                        }
                    }

                    public void remove() { }
                };
            }
        }

        public Iterator<ResultRow> getResults() {
            return new Iterator<ResultRow>() {
                Iterator<StructuredResultRow> sri = r.getResults().iterator();
                public boolean hasNext() {
                    return sri.hasNext();
                }

                public ResultRow next() {
                    return new ResultRow(sri.next());
                }

                public void remove() {}
            };
        }
    }

    public static class ErrorResult {
        private String error;

        public ErrorResult(String error) {
            this.error = error;
        }

        public @RestOut String getError() {
            return error;
        }
    }

    public Object process(HttpServletRequest request) {
        final String oneGeneId = request.getParameter("oneGene");
        if(oneGeneId != null) {            
            AtlasDao dao = ArrayExpressSearchService.instance().getAtlasDao();
            AtlasDao.AtlasGeneResult geneResult = dao.getGeneByIdentifier(oneGeneId);
            if(!geneResult.isFound()) {
                return new ErrorResult("No such gene found for " + oneGeneId);
            } else if(geneResult.isMulti()) {
                return new ErrorResult("Multiple gene found for " + oneGeneId);
            } else {
                geneResult.getGene().loadGeneExperiments(dao);
                setRestProfile(GeneViewRestProfile.class);
                return geneResult.getGene();
            }            
        } else {
            final AtlasStructuredQueryService asqs = ArrayExpressSearchService.instance().getStructQueryService();

            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(request,
                    GeneProperties.allPropertyIds(),
                    asqs.getExperimentalFactors());

            if(!atlasQuery.isNone()) {
                AtlasStructuredQueryResult atlasResult = asqs.doStructuredAtlasQuery(atlasQuery);
                setRestProfile(ListViewRestProfile.class);
                return atlasQuery.getViewType() == ViewType.HEATMAP ? new HeatmapResultAdapter(atlasResult) : atlasResult;
            } else {
                return new ErrorResult("Empty query specified");
            }
        }
    }

}

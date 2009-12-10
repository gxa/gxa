package ae3.servlet.structuredquery;

import ae3.service.structuredquery.*;
import ae3.util.HtmlHelper;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

import uk.ac.ebi.gxa.web.AtlasSearchService;
import uk.ac.ebi.gxa.web.Atlas;
import uk.ac.ebi.gxa.model.impl.AtlasDao;
import uk.ac.ebi.gxa.model.impl.ExpressionStatDao;
import uk.ac.ebi.gxa.model.*;
import uk.ac.ebi.gxa.utils.Pair;
import org.apache.commons.lang.StringUtils;

/**
 * @author pashky
 */
public class NewStructuredQueryServlet extends HttpServlet {
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    public static class ResultWrapper {
        private EfvTree<Boolean> resultEfvs = new EfvTree<Boolean>();
        private int total;
        private AtlasStructuredQuery atlasQuery;

        private Map<String, Experiment> experimentMap = new HashMap<String, Experiment>();

        private List<Pair<Map<String, String>,SortedMap<EfvTree.EfEfv, ExpressionStat>>> results = new ArrayList<Pair<Map<String, String>, SortedMap<EfvTree.EfEfv, ExpressionStat>>>();

        public ResultWrapper(FacetQueryResultSet<GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>>, ExpressionStatFacet> results,
                             Dao dao,
                             EfvTree<Boolean> queryEfvs,
                             AtlasStructuredQuery atlasQuery) throws GxaException {
            final boolean hasQueryEfvs = queryEfvs.getNumEfvs() > 0;

            this.atlasQuery = atlasQuery;
            this.total = results.getTotalResults();

            if(hasQueryEfvs)
                resultEfvs = queryEfvs;

            for(GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>> ge : results.getItems()) {
                SortedMap<EfvTree.EfEfv, ExpressionStat> exprs = new TreeMap<EfvTree.EfEfv, ExpressionStat>();
                for(PropertyExpressionStat<ExperimentExpressionStat> pe : ge.getDrillDown()) {
                    final String ef = pe.getProperty().getAccession();
                    final String efv = pe.getProperty().getValues().iterator().next();
                    EfvTree.EfEfv efEfv = hasQueryEfvs ? new EfvTree.EfEfv<Boolean>(ef, efv, true) : resultEfvs.put(ef, efv, true);
                    if(!hasQueryEfvs || resultEfvs.has(ef, efv))
                        exprs.put(efEfv, pe);

                    for(ExperimentExpressionStat ee : pe.getDrillDown()) {
                        if(!experimentMap.containsKey(ee.getExperiment())) {
                            try {
                                Experiment experiment = dao.getExperiment(new ExperimentQuery().hasId(ee.getExperiment())).getItem();
                                experimentMap.put(ee.getExperiment(), experiment);
                            } catch (GxaException e) {
                                // okay
                            }
                        }
                    }
                }

                Map<String, String> geneProps = new HashMap<String, String>();
                Gene gene = dao.getGene(new GeneQuery().hasId(ge.getGene())).getItem();
                for(Property property : gene.getProperties().getProperties()) {
                    geneProps.put(property.getAccession(), StringUtils.join(property.getValues(), ", "));
                }
                geneProps.put("name", gene.getAccession());
                geneProps.put("id", String.valueOf(gene.getId()));
                geneProps.put("species", gene.getSpecies());
                this.results.add(new Pair<Map<String, String>, SortedMap<EfvTree.EfEfv, ExpressionStat>>(geneProps, exprs));
            }
        }

        public Map<String, Experiment> getExperimentMap() {
            return experimentMap;
        }

        public int getSize() {
            return results.size();
        }

        public int getTotal() {
            return total;
        }

        public int getPage() {
            return atlasQuery.getStart() / getRowsPerPage();
        }

        public int getRowsPerPage() {
            return atlasQuery.getRowsPerPage();
        }

        public EfvTree<Boolean> getResultEfvs() {
            return resultEfvs;
        }

        public List<Pair<Map<String, String>, SortedMap<EfvTree.EfEfv, ExpressionStat>>> getResults() {
            return results;
        }
    }

    AtlasDao dao;

    private void doIt(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        long startTime = HtmlHelper.currentTime();

        AtlasSearchService searchService =
                (AtlasSearchService) getServletContext().getAttribute(Atlas.SEARCH_SERVICE.key());

        if(dao == null) {
            dao = new AtlasDao();
            try {
                dao.Connect("jdbc:oracle:thin:@apu.ebi.ac.uk:1521:AEDWT", "Atlas2",  "Atlas2");
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
            ExpressionStatDao esd = new ExpressionStatDao();
            esd.setGeneServer(searchService.getAtlasSolrServer());
            esd.setDao(dao);
            dao.setExpressionStatDao(esd);
        }


        try {
            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRequest(request);

            if (!atlasQuery.isNone()) {
                ExpressionStatQuery query = new ExpressionStatQuery();

                GeneQuery gq = new GeneQuery();
                for(GeneQueryCondition g : atlasQuery.getGeneConditions()) {
                    PropertyQuery propq = new PropertyQuery().hasAccession(g.getFactor());
                    for(String v : g.getFactorValues())
                        propq.fullTextQuery(v);
                    if(g.isNegated())
                        gq.hasNotProperty(propq);
                    else
                        gq.hasProperty(propq);
                }
                query.hasGene(gq);

                for(ExpFactorQueryCondition e : atlasQuery.getConditions()) {
                    PropertyQuery propq = new PropertyQuery().hasAccession(e.getFactor());
                    for(String v : e.getFactorValues())
                        propq.fullTextQuery(v);
                    if(e.getExpression() == QueryExpression.UP)
                        query.activeIn(ExpressionQuery.UP, propq);
                    else if(e.getExpression() == QueryExpression.DOWN)
                        query.activeIn(ExpressionQuery.DOWN, propq);
                    else if(e.getExpression() == QueryExpression.UP_DOWN)
                        query.activeIn(ExpressionQuery.UP_OR_DOWN, propq);
                }

                if(!atlasQuery.getSpecies().isEmpty())
                    for(String s : atlasQuery.getSpecies())
                        query.hasGene(new GeneQuery().hasSpecies(s));

                PageSortParams params = new PageSortParams();
                params.setRows(atlasQuery.getRowsPerPage());
                params.setStart(atlasQuery.getStart());
                // TODO: params.setWhat??(atlasQuery.getExpsPerGene())

                query.setFacets(true);
                FacetQueryResultSet<GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>>, ExpressionStatFacet> result = dao.getExpressionStat(query, params);

                EfvTree<Boolean> queryEfvs = new EfvTree<Boolean>();
                for(Pair<ExpressionQuery,PropertyQuery> propq : query.getActivityQueries()) {
                    QueryResultSet<Property> props = dao.getProperty(propq.getSecond().isAssayProperty(true), PageSortParams.ALL);
                    for(Property property : props.getItems()) {
                        for(String value : property.getValues())
                            queryEfvs.put(property.getAccession(), value, true);
                    }
                }
                
                request.setAttribute("result", new ResultWrapper(result, dao, queryEfvs, atlasQuery));
            }

            request.setAttribute("query", atlasQuery);
            request.setAttribute("timeStart", startTime);
            request.setAttribute("heatmap", atlasQuery.getViewType() == ViewType.HEATMAP);
            request.setAttribute("list", atlasQuery.getViewType() == ViewType.LIST);
            request.setAttribute("forcestruct", request.getParameter("struct") != null);
            request.setAttribute("service", searchService);

        } catch (GxaException gxae) {
            throw new RuntimeException(gxae);
        }

        request.getRequestDispatcher("new-structured-query.jsp").forward(request, response);
    }
}

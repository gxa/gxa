package ae3.servlet.structuredquery;

import ae3.service.structuredquery.*;

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
import uk.ac.ebi.gxa.utils.EscapeUtil;
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

    private static final String PARAM_EXPRESSION = "fexp_";
    private static final String PARAM_FACTOR = "fact_";
    private static final String PARAM_FACTORVALUE = "fval_";
    private static final String PARAM_GENE = "gval_";
    private static final String PARAM_GENENOT = "gnot_";
    private static final String PARAM_GENEPROP = "gprop_";
    private static final String PARAM_SPECIE = "specie_";
    private static final String PARAM_START = "p";
    private static final String PARAM_EXPAND = "fexp";

    public static class ResultWrapper {
        private EfvTree<Boolean> resultEfvs = new EfvTree<Boolean>();
        private int total;

        private List<Pair<Map<String, String>,Map<EfvTree.EfEfv, ExpressionStat>>> results = new ArrayList<Pair<Map<String, String>, Map<EfvTree.EfEfv, ExpressionStat>>>();

        public ResultWrapper(FacetQueryResultSet<GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>>, ExpressionStatFacet> results, Dao dao, EfvTree<Boolean> queryEfvs) {
            final boolean hasQueryEfvs = queryEfvs.getNumEfvs() > 0;

            total = results.getTotalResults();

            if(hasQueryEfvs)
                resultEfvs = queryEfvs;

            for(GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>> ge : results.getItems()) {
                Map<EfvTree.EfEfv, ExpressionStat> exprs = new HashMap<EfvTree.EfEfv, ExpressionStat>();
                for(PropertyExpressionStat pe : ge.drillDown()) {
                    final String ef = pe.getProperty().getAccession();
                    final String efv = pe.getProperty().getValues().iterator().next();
                    EfvTree.EfEfv efEfv = hasQueryEfvs ? new EfvTree.EfEfv<Boolean>(ef, efv, true) : resultEfvs.put(ef, efv, true);
                    if(!hasQueryEfvs || resultEfvs.has(ef, efv))
                        exprs.put(efEfv, pe);
                }

                Map<String, String> geneProps = new HashMap<String, String>();
                try {
                    Gene gene = dao.getGene(new GeneQuery().hasId(ge.getGene())).getItem();
                    for(Property property : gene.getProperties().getProperties()) {
                        geneProps.put(property.getAccession(), StringUtils.join(property.getValues(), ", "));
                    }
                    geneProps.put("name", gene.getAccession());
                    geneProps.put("id", String.valueOf(gene.getId()));
                    geneProps.put("species", gene.getSpecies());
                    this.results.add(new Pair<Map<String, String>, Map<EfvTree.EfEfv, ExpressionStat>>(geneProps, exprs));
                } catch(GxaException gxae) {
                    throw new RuntimeException(gxae);
                }
            }
        }

        public int getSize() {
            return results.size();
        }

        public int getTotal() {
            return total;
        }

        public int getPage() {
            return 0;
        }

        public int getRowsPerPage() {
            return 100;
        }

        public EfvTree<Boolean> getResultEfvs() {
            return resultEfvs;
        }

        public List<Pair<Map<String, String>, Map<EfvTree.EfEfv, ExpressionStat>>> getResults() {
            return results;
        }
    }

    AtlasDao dao;

    private void doIt(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

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
            boolean hasQuery = false;
            PageSortParams params = new PageSortParams();
            params.setRows(100);
            params.setStart(0);

            ExpressionStatQuery query = new ExpressionStatQuery();

            GeneQuery gq = new GeneQuery();
            for(String id : AtlasStructuredQueryParser.findPrefixParamsSuffixes(request, PARAM_GENEPROP)) {
                try {
                    String nots = request.getParameter(PARAM_GENENOT + id);
                    boolean not = nots != null && !"".equals(nots) && !"0".equals(nots);

                    String factor = request.getParameter(PARAM_GENEPROP + id);
                    if(factor == null)
                        throw new IllegalArgumentException("Empty gene property name rowid:" + id);

                    String value = request.getParameter(PARAM_GENE + id);
                    List<String> values = value != null ? EscapeUtil.parseQuotedList(value) : new ArrayList<String>();
                    if(values.size() > 0)
                    {
                        PropertyQuery propq = new PropertyQuery().hasAccession(factor);
                        for(String v : values)
                            propq.fullTextQuery(v);
                        if(not)
                            gq.hasNotProperty(propq);
                        else
                            gq.hasProperty(propq);
                    }
                    hasQuery = true;
                } catch (IllegalArgumentException e) {
                    // Ignore this one, may be better stop future handling
                }
            }

            query.hasGene(gq);            

            for(String id : AtlasStructuredQueryParser.findPrefixParamsSuffixes(request, PARAM_FACTOR)) {
                try {
                    String exps = request.getParameter(PARAM_EXPRESSION + id);
                    ExpressionQuery exp;
                    if(exps.equalsIgnoreCase("UP"))
                        exp = ExpressionQuery.UP;
                    else if(exps.equalsIgnoreCase("DOWN"))
                        exp = ExpressionQuery.DOWN;
                    else
                        exp = ExpressionQuery.UP_OR_DOWN;

                    String factor = request.getParameter(PARAM_FACTOR + id);
                    if(factor == null)
                        throw new IllegalArgumentException("Empty factor name rowid:" + id);

                    String value = request.getParameter(PARAM_FACTORVALUE + id);
                    List<String> values = value != null ? EscapeUtil.parseQuotedList(value) : new ArrayList<String>();

                    if(values.size() > 0)
                    {
                        PropertyQuery propq = new PropertyQuery().hasAccession(factor);
                        for(String v : values)
                            propq.fullTextQuery(v);
                        query.activeIn(exp, propq);
                    }
                    hasQuery = true;
                } catch (IllegalArgumentException e) {
                    // Ignore this one, may be better stop future handling
                }
            }

            if(hasQuery) {
                FacetQueryResultSet<GeneExpressionStat<PropertyExpressionStat<ExperimentExpressionStat>>, ExpressionStatFacet> result = dao.getExpressionStat(query, params);

                EfvTree<Boolean> queryEfvs = new EfvTree<Boolean>();
                for(Pair<ExpressionQuery,PropertyQuery> propq : query.getActivityQueries()) {
                    QueryResultSet<Property> props = dao.getProperty(propq.getSecond().isAssayProperty(true), PageSortParams.ALL);
                    for(Property property : props.getItems()) {
                        for(String value : property.getValues())
                            queryEfvs.put(property.getName(), value, true);
                    }
                }
                request.setAttribute("result", new ResultWrapper(result, dao, queryEfvs));
            }

        } catch (GxaException gxae) {
            throw new RuntimeException(gxae);
        }

        request.setAttribute("heatmap", true);
        request.getRequestDispatcher("new-structured-query.jsp").forward(request, response);
    }
}

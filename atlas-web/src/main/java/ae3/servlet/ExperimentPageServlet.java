package ae3.servlet;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ListResultRow;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author pashky
 */
public class ExperimentPageServlet implements HttpRequestHandler {

    private AtlasDao dao;
    private AtlasStructuredQueryService queryService;

    public void setDao(AtlasDao dao) {
        this.dao = dao;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String expAcc = request.getParameter("eid");
        String geneId = request.getParameter("gid");
        String ef = request.getParameter("ef");

        if (expAcc != null && !"".equals(expAcc)) {
            AtlasExperiment exp = dao.getExperimentByAccession(expAcc);
            if (exp != null) {
                request.setAttribute("exp", exp);
                request.setAttribute("eid", exp.getId());

                List<ListResultRow> topGenes = queryService.findGenesForExperiment("", exp.getId(), 0, 10);
                request.setAttribute("geneList", topGenes);

                Collection<AtlasGene> genes = new ArrayList<AtlasGene>();
                if(geneId != null) {
                    for(String geneQuery : StringUtils.split(geneId, ",")) {
                        AtlasDao.AtlasGeneResult result = dao.getGeneByIdentifier(geneQuery);
                        if (result.isFound()) {
                            genes.add(result.getGene());
                            if (ef == null || "".equals(ef)) {
                                ef = result.getGene().getHighestRankEF(exp.getId()).getFirst();
                                request.setAttribute("topRankEF", ef);
                            }
                        }
                    }
                } else {
                    for(ListResultRow lrr : topGenes.size() > 5 ? topGenes.subList(0, 5) : topGenes)
                        genes.add(lrr.getGene());
                }
                request.setAttribute("genes", genes);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                request.setAttribute("errorMessage", "There are no records for experiment " + String.valueOf(expAcc));
                request.getRequestDispatcher("/error.jsp").forward(request,response);
            }
        }

        request.setAttribute("ef", ef);
        request.getRequestDispatcher("/experiment.jsp").forward(request, response);
    }
}

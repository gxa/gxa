package uk.ac.ebi.gxa.requesthandlers.genepage;

import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;

/**
 * Experiment list in gene page
 * @author pashky
 */
public class ExperimentsListRequestHandler implements HttpRequestHandler {
    private AtlasDao atlasSolrDao;

    public AtlasDao getAtlasSolrDao() {
        return atlasSolrDao;
    }

    public void setAtlasSolrDao(AtlasDao atlasSolrDao) {
        this.atlasSolrDao = atlasSolrDao;
    }


    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String geneId = request.getParameter("gid");
        if (geneId != null) {
            int fromRow = -1;
            try { fromRow = Integer.valueOf(request.getParameter("from")); } catch (Exception e) { }
            int toRow = -1;
            try { toRow = Integer.valueOf(request.getParameter("to")); } catch (Exception e) { }

            String ef = request.getParameter("factor");
            String efv = request.getParameter("efv");

            AtlasDao.AtlasGeneResult atlasGene = atlasSolrDao.getGeneByIdentifier(geneId);
            if(atlasGene.isFound()) {
                List<AtlasExperiment> exps = atlasSolrDao.getRankedGeneExperiments(atlasGene.getGene(), ef, efv,  fromRow, toRow);
                request.setAttribute("exps",exps);
                request.setAttribute("atlasGene", atlasGene.getGene());
                request.getRequestDispatcher("/WEB-INF/jsp/genepage/experiment-list.jsp").include(request, response);
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}

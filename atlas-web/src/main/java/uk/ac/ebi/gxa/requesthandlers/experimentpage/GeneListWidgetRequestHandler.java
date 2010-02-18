package uk.ac.ebi.gxa.requesthandlers.experimentpage;

import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.util.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.experimentpage.result.SimilarityResultSet;
import org.kchine.r.RDataFrame;
import org.kchine.r.server.RServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;

public class GeneListWidgetRequestHandler implements HttpRequestHandler {
    final private Logger log = LoggerFactory.getLogger(getClass());
    private static final int NUM_GENES = AtlasProperties.getIntProperty("atlas.query.listsize");

    AtlasStructuredQueryService queryService;
    AtlasComputeService computeService;

    public AtlasStructuredQueryService getQueryService() {
        return queryService;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public AtlasComputeService getComputeService() {
        return computeService;
    }

    public void setComputeService(AtlasComputeService computeService) {
        this.computeService = computeService;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int eid = Integer.valueOf(request.getParameter("eid"));
        String qryType = request.getParameter("query");
        String geneId = request.getParameter("gid");
        String startRow = request.getParameter("from");
        Integer start;
        try {
            start = Integer.valueOf(startRow);
        }
        catch (NumberFormatException e) {
            start = 0;
        }

        Object geneQuery = null;
        if (qryType.equals("sim")) {
            String DEid = request.getParameter("deid");
            String ADid = request.getParameter("adid");
            final SimilarityResultSet simRS = new SimilarityResultSet(String.valueOf(eid), DEid, ADid);

            try {
                RDataFrame sim = computeService.computeTask(new ComputeTask<RDataFrame>() {

                    public RDataFrame compute(RServices R) throws RemoteException {
                        // load resource - this is not specially initialized anymore - fixme?
                        R.sourceFromResource("sim.R");
                        String callSim = "sim.nc(" + 
                                simRS.getTargetDesignElementId() + ",'" +
                                simRS.getSourceNetCDF() + "')";
                        return (RDataFrame) R.getObject(callSim);
                    }
                });

                if (null != sim) {
                    simRS.loadResult(sim);
                    geneQuery = simRS.getSimGeneIDs();
                    request.setAttribute("simRS", simRS);
                }
            }
            catch (Exception e) {
                log.error("Problem computing similarity!", e.getMessage());
                return;
            }

        } else if (qryType.equals("top")) {
            geneQuery = "";
        } else if (qryType.equals("search")) {
            geneQuery = request.getParameter("gene");
        }

        if(geneQuery != null) {
            request.setAttribute("geneList", queryService.findGenesForExperiment(geneQuery, eid, start, NUM_GENES));
        }

        request.setAttribute("eid", eid);
        request.setAttribute("gid", geneId);

        request.getRequestDispatcher("/WEB-INF/jsp/experimentpage/gene-list.jsp").forward(request, response);
    }
}

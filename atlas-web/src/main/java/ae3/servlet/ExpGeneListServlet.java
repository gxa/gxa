package ae3.servlet;

import ae3.model.ListResultRow;
import ae3.service.structuredquery.AtlasStructuredQueryResult;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.util.AtlasProperties;
import org.kchine.r.RDataFrame;
import org.kchine.r.server.RServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

public class ExpGeneListServlet implements HttpRequestHandler {
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
        String eAcc = request.getParameter("eAcc");
        String eid = request.getParameter("eid");
        String qryType = request.getParameter("query");
        String geneId = request.getParameter("gid");
        String startRow = request.getParameter("from");
        Integer start;
        AtlasStructuredQueryResult result = new AtlasStructuredQueryResult(0, 0, 0);
        try {
            start = Integer.valueOf(startRow);
        }
        catch (NumberFormatException e) {
            start = 0;
        }

        if (qryType.equals("sim")) {
            String DEid = request.getParameter("deid");
            String ADid = request.getParameter("adid");
            final SimilarityResultSet simRS = new SimilarityResultSet(eid, DEid, ADid);

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
                    ArrayList<String> simGeneIds = simRS.getSimGeneIDs();
                    result = queryService.findGenesForExperiment(simGeneIds, eAcc, start, NUM_GENES);
                    request.setAttribute("genes", result.getListResults());
                    request.setAttribute("simRS", simRS);
                }
            }
            catch (Exception e) {
                log.error("Problem computing similarity!", e.getMessage());
                return;
            }

        } else if (qryType.equals("top")) {

            result = queryService.findGenesForExperiment("", eAcc, start, NUM_GENES);

            Collection<ListResultRow> a = result.getListResults();

            request.setAttribute("genes", a);

        } else if (qryType.equals("search")) {
            String geneQuery = request.getParameter("gene");
            result = queryService.findGenesForExperiment(geneQuery != null ? geneQuery : "", eAcc, start, NUM_GENES);
            request.setAttribute("genes", result.getListResults());
        }
        request.setAttribute("result", result);
        request.setAttribute("eAcc", eAcc);
        request.setAttribute("eid", eid);
        request.setAttribute("gid", geneId);

        request.getRequestDispatcher("/expGeneResults.jsp").forward(request, response);
    }
}

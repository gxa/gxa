package ae3.servlet;

import ae3.service.ArrayExpressSearchService;
import ae3.service.compute.AtlasComputeService;
import ae3.service.compute.ComputeTask;
import ae3.service.structuredquery.AtlasStructuredQueryResult;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.util.AtlasProperties;
import ae3.model.ListResultRow;
import ds.server.SimilarityResultSet;
import org.kchine.r.RDataFrame;
import org.kchine.r.server.RServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

public class ExpGeneListServlet extends HttpServlet {
	final private Logger log = LoggerFactory.getLogger(getClass());
    private static final int NUM_GENES = AtlasProperties.getIntProperty("atlas.query.listsize");

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
		doIt(httpServletRequest, httpServletResponse);
	}

	protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
		doIt(httpServletRequest, httpServletResponse);
	}

	private void doIt(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String eAcc = request.getParameter("eAcc");
		String eid = request.getParameter("eid");
		String qryType = request.getParameter("query");
		String geneId = request.getParameter("gid");
		String startRow = request.getParameter("from");
		Integer start;
		AtlasStructuredQueryResult result = new AtlasStructuredQueryResult(0,0,0);
		try{
			start = Integer.valueOf(startRow);
		}catch (NumberFormatException e){
			start = 0;
		}

        AtlasStructuredQueryService service = ArrayExpressSearchService.instance().getStructQueryService();

		if(qryType.equals("sim")){
			String DEid = request.getParameter("deid");
			String ADid = request.getParameter("adid");
			final SimilarityResultSet simRS = new SimilarityResultSet(eid,DEid,ADid);

			try {
                AtlasComputeService computeService = ae3.service.ArrayExpressSearchService.instance().getComputeService();
                RDataFrame sim = computeService.computeTask(new ComputeTask<RDataFrame>() {

                    public RDataFrame compute(RServices R) throws RemoteException {
                        String callSim = "sim.nc(" + simRS.getTargetDesignElementId() + ",'" + simRS.getSourceNetCDF() + "')";
                        return (RDataFrame) R.getObject(callSim);
                    }
                });

                if(null != sim) {
                    simRS.loadResult(sim);
					ArrayList<String> simGeneIds = simRS.getSimGeneIDs();
					result = service.findGenesForExperiment(simGeneIds,eAcc,start, NUM_GENES);
					request.setAttribute("genes",result.getListResults());
					request.setAttribute("simRS", simRS);
				}
			} catch (Exception e) {
				log.error("Problem computing similarity!", e.getMessage());
				return;
			} 

		}else if(qryType.equals("top")){

			result = service.findGenesForExperiment("", eAcc,start, NUM_GENES);

            Collection<ListResultRow> a = result.getListResults();

			request.setAttribute("genes", a);

		}else if(qryType.equals("search")){
			String geneQuery = request.getParameter("gene");
			result = service.findGenesForExperiment(geneQuery != null ? geneQuery : "",eAcc,start, NUM_GENES);
			request.setAttribute("genes",result.getListResults());
		}
		request.setAttribute("result", result);
		request.setAttribute("eAcc", eAcc);
		request.setAttribute("eid", eid);
		request.setAttribute("gid", geneId);

		request.getRequestDispatcher("/expGeneResults.jsp").forward(request, response);
	}
}

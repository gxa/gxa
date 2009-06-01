package ae3.servlet;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.kchine.rpf.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ds.biocep.Similarity;
import ds.server.SimilarityResultSet;

import ae3.service.ArrayExpressSearchService;
import ae3.service.ExperimentService;
import ae3.service.structuredquery.AtlasStructuredQueryResult;
import ae3.util.EscapeUtil;

public class ExpGeneListServlet extends HttpServlet {
	final private Logger log = LoggerFactory.getLogger(getClass());

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
		AtlasStructuredQueryResult result = new AtlasStructuredQueryResult(0,0);
		try{
			start = Integer.valueOf(startRow);
		}catch (NumberFormatException e){
			start = 0;
		}

		if(qryType.equals("sim")){
			String DEid = request.getParameter("deid");
			String ADid = request.getParameter("adid");
			SimilarityResultSet simRS = new SimilarityResultSet(eid,DEid,ADid);

			try {
				if(Similarity.getSimilarDEs(simRS)){
					ArrayList<String> simGeneIds = simRS.getSimGeneIDs();
					result = ExperimentService.getGenesForExperiment(simGeneIds,eAcc,start);
					request.setAttribute("genes",result.getListResults());
					request.setAttribute("simRS", simRS);
				}
			} catch (Exception e) {
				log.error(e.getMessage());
				return;
			} 

		}else if(qryType.equals("top")){

			result = ExperimentService.getGenesForExperiment(new ArrayList<String>(), eAcc,start);
			request.setAttribute("genes", result.getListResults());

		}else if(qryType.equals("search")){
			String geneQuery = request.getParameter("gene");
			result = ExperimentService.getGenesForExperiment((ArrayList)EscapeUtil.parseQuotedList(geneQuery),eAcc,start);
			request.setAttribute("genes",result.getListResults());
		}
		request.setAttribute("result", result);
		request.setAttribute("eAcc", eAcc);
		request.setAttribute("eid", eid);
		request.setAttribute("gid", geneId);

		request.getRequestDispatcher("/expGeneResults.jsp").forward(request, response);
	}
}

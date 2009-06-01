package ae3.servlet.structuredquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;

import ae3.service.ArrayExpressSearchService;
import ae3.service.DownloadService;
import ae3.service.structuredquery.AtlasStructuredQueryResult;
import ae3.service.structuredquery.AtlasStructuredQueryParser;
import ae3.service.structuredquery.AtlasStructuredQuery;
import ae3.service.structuredquery.StructuredResultRow;
import ae3.util.HtmlHelper;

/**
 * @author pashky
 */
public class StructuredQueryServlet extends HttpServlet {
    final private Logger log = LoggerFactory.getLogger(getClass());

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    private void doIt(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        long startTime = HtmlHelper.currentTime(); 

        AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRequest(request);
        
        if(!atlasQuery.isNone()) {
            
        	if(atlasQuery.isExport()){
        		DownloadService.requestDownload(request.getSession(),atlasQuery);
    	        return;
            }else{
            	AtlasStructuredQueryResult atlasResult = ArrayExpressSearchService.instance().getStructQueryService().doStructuredAtlasQuery(atlasQuery);
            	request.setAttribute("result", atlasResult);
                
                
                if(atlasResult.getSize() == 1) {
                    StructuredResultRow row = atlasResult.getResults().iterator().next();
                    String url = "gene?gid=" + row.getGene().getGeneIdentifier();
                    response.sendRedirect(url);
                    return;
                }
            }

        }

        request.setAttribute("query", atlasQuery);
        request.setAttribute("timeStart", startTime);
        request.setAttribute("heatmap", atlasQuery.viewHeatMap());
        request.setAttribute("list", atlasQuery.viewList());
        request.setAttribute("forcestruct", request.getParameter("struct") != null);
        request.setAttribute("service", ArrayExpressSearchService.instance());
        request.setAttribute("noDownloads", DownloadService.getNumOfDownloads(request.getSession().getId()));

        request.getRequestDispatcher("structured-query.jsp").forward(request, response);
    }
}

package ae3.servlet.structuredquery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

import ae3.service.ArrayExpressSearchService;
import ae3.service.structuredquery.AtlasStructuredQueryResult;
import ae3.service.structuredquery.AtlasStructuredQueryParser;
import ae3.service.structuredquery.AtlasStructuredQuery;
import ae3.service.structuredquery.StructuredResultRow;
import ae3.util.HtmlHelper;

/**
 * @author pashky
 */
public class StructuredQueryServlet extends HttpServlet {
    private Log log = LogFactory.getLog(getClass());

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
            AtlasStructuredQueryResult atlasResult = ArrayExpressSearchService.instance().getStructQueryService().doStructuredAtlasQuery(atlasQuery);

            if(atlasResult.getSize() == 1) {
                StructuredResultRow row = atlasResult.getResults().iterator().next();
                String url = "gene?gid=" + row.getGene().getGeneIdentifier();
                response.sendRedirect(url);
                return;
            }
            
            request.setAttribute("result", atlasResult);
        }

        request.setAttribute("query", atlasQuery);
        request.setAttribute("timeStart", startTime);
        request.setAttribute("heatmap", "hm".equals(request.getParameter("view")));
        request.setAttribute("forcestruct", request.getParameter("struct") != null);
        request.setAttribute("service", ArrayExpressSearchService.instance());

        request.getRequestDispatcher("structured-query.jsp").forward(request, response);
    }
}

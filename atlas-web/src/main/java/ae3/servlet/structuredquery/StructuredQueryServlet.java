package ae3.servlet.structuredquery;

import ae3.service.AtlasDownloadService;
import ae3.service.structuredquery.*;
import ae3.util.HtmlHelper;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author pashky
 */
public class StructuredQueryServlet extends HttpServlet {
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    private void doIt(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        long startTime = HtmlHelper.currentTime();

        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        AtlasStructuredQueryService queryService = (AtlasStructuredQueryService)context.getBean("atlasQueryService");
        AtlasDownloadService downloadService = (AtlasDownloadService)context.getBean("atlasDownloadService");

        AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRequest(request);

        if (!atlasQuery.isNone()) {
            if (request.getParameter("export") != null && request.getParameter("export").equals("true")) {
                int queryId = downloadService.requestDownload(request.getSession(), atlasQuery);
                response.getOutputStream().print("{qid:" + queryId + "}");
                return;
            }
            else {
                AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
                request.setAttribute("result", atlasResult);

                if (atlasResult.getSize() == 1) {
                    StructuredResultRow row = atlasResult.getResults().iterator().next();
                    String url = "gene?gid=" + row.getGene().getGeneIdentifier();
                    response.sendRedirect(url);
                    return;
                }
            }

        }

        request.setAttribute("query", atlasQuery);
        request.setAttribute("timeStart", startTime);
        request.setAttribute("heatmap", atlasQuery.getViewType() == ViewType.HEATMAP);
        request.setAttribute("list", atlasQuery.getViewType() == ViewType.LIST);
        request.setAttribute("forcestruct", request.getParameter("struct") != null);
        request.setAttribute("noDownloads", downloadService.getNumOfDownloads(request.getSession().getId()));

        request.getRequestDispatcher("structured-query.jsp").forward(request, response);
    }
}

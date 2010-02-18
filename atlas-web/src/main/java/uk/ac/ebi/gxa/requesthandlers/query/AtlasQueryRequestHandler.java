package uk.ac.ebi.gxa.requesthandlers.query;

import ae3.service.AtlasDownloadService;
import ae3.service.structuredquery.*;
import ae3.util.HtmlHelper;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;

/**
 * @author pashky
 */
public class AtlasQueryRequestHandler implements HttpRequestHandler, IndexBuilderEventHandler {

    private AtlasStructuredQueryService queryService;
    private AtlasDownloadService downloadService;
    private boolean disableQueries = false;

    public AtlasStructuredQueryService getQueryService() {
        return queryService;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public AtlasDownloadService getDownloadService() {
        return downloadService;
    }

    public void setDownloadService(AtlasDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void onIndexBuildFinish(IndexBuilder builder, IndexBuilderEvent event) {
        disableQueries = false;
    }

    public void onIndexBuildStart(IndexBuilder builder) {
        disableQueries = true;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if(disableQueries) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            request.setAttribute("errorMessage", "Index building is in progress, please wait");
            request.getRequestDispatcher("/error.jsp").forward(request,response);
        }

        long startTime = HtmlHelper.currentTime();

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

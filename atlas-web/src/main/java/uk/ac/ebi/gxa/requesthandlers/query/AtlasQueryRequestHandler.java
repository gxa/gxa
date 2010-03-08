/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.requesthandlers.query;

import ae3.service.AtlasDownloadService;
import ae3.service.structuredquery.*;
import ae3.util.HtmlHelper;
import org.springframework.web.HttpRequestHandler;
import org.springframework.beans.factory.DisposableBean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;
import uk.ac.ebi.gxa.properties.AtlasProperties;

/**
 * @author pashky
 */
public class AtlasQueryRequestHandler implements HttpRequestHandler, IndexBuilderEventHandler, DisposableBean {

    private AtlasStructuredQueryService queryService;
    private AtlasDownloadService downloadService;
    private AtlasProperties atlasProperties;
    private IndexBuilder indexBuilder;

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
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void onIndexBuildFinish(IndexBuilder builder, IndexBuilderEvent event) {
        disableQueries = false;
    }

    public void onIndexBuildStart(IndexBuilder builder) {
        disableQueries = true;
    }

    public void destroy() throws Exception {
        if(indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if(disableQueries) {
            ErrorResponseHelper.errorUnavailable(request, response, "Index building is in progress, please wait");
            return;
        }

        long startTime = HtmlHelper.currentTime();

        AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRequest(request, atlasProperties);

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

        request.getRequestDispatcher("/WEB-INF/jsp/query/query-result.jsp").forward(request, response);
    }
}

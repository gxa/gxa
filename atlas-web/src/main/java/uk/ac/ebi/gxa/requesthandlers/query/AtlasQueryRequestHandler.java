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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.lang.System.currentTimeMillis;

/**
 * @author pashky
 */
public class AtlasQueryRequestHandler implements HttpRequestHandler, IndexBuilderEventHandler, DisposableBean {

    private AtlasStructuredQueryService queryService;
    private AtlasDownloadService downloadService;
    private AtlasProperties atlasProperties;
    private IndexBuilder indexBuilder;

    private boolean disableQueries = false;

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
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

    public void onIndexBuildFinish() {
        disableQueries = false;
    }

    public void onIndexBuildStart() {
        disableQueries = true;
    }

    public void destroy() throws Exception {
        if (indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (disableQueries) {
            ErrorResponseHelper.errorUnavailable(request, response, "Index building is in progress, please wait");
            return;
        }

        long startTime = currentTimeMillis();

        AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRequest(request, atlasProperties);
        AtlasStructuredQueryResult atlasResult;

        if (!atlasQuery.isNone()) {
            if (request.getParameter("export") != null && request.getParameter("export").equals("true")) {
                int queryId = downloadService.requestDownload(request.getSession(), atlasQuery);
                response.setContentType("application/json");
                response.setCharacterEncoding("utf-8");
                response.getOutputStream().print("{\"qid\":" + queryId + "}");
                return;
            }

            atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
        } else {
            atlasResult = new AtlasStructuredQueryResult(0, 0, 0);
        }

        request.setAttribute("result", atlasResult);
        request.setAttribute("query", atlasQuery);
        request.setAttribute("timeStart", startTime);
        request.setAttribute("heatmap", atlasQuery.getViewType() == ViewType.HEATMAP);
        request.setAttribute("list", atlasQuery.getViewType() == ViewType.LIST);
        request.setAttribute("forcestruct", request.getParameter("struct") != null);
        request.setAttribute("noDownloads", downloadService.getNumOfDownloads(request.getSession().getId()));

        request.getRequestDispatcher("/WEB-INF/jsp/query/query-result.jsp").forward(request, response);
    }
}

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

package uk.ac.ebi.gxa.requesthandlers.api.v2;

import ae3.dao.ExperimentSolrDAO;
import ae3.dao.GeneSolrDAO;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.BioEntityDAO;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.FieldFilter;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.JsonRestResultRenderer;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.MapBasedFieldFilter;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestResultRenderException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * REST API v2 structured query servlet. Handles all gene and experiment API queries according to HTTP request parameters
 */
public class QueryRequestHandler implements HttpRequestHandler, /*IndexBuilderEventHandler,*/ DisposableBean {
    private Logger log = LoggerFactory.getLogger(getClass());

    private BioEntityDAO bioEntityDAO;
    private ExperimentSolrDAO experimentSolrDAO;
    private GeneSolrDAO geneSolrDAO;
    private AtlasDAO atlasDAO;
    private AtlasDataDAO atlasDataDAO;

    public void setExperimentSolrDAO(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }

    public void setBioEntityDAO(BioEntityDAO bioEntityDAO) {
        this.bioEntityDAO = bioEntityDAO;
    }

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAtlasDataDAO(AtlasDataDAO atlasDataDAO) {
        this.atlasDataDAO = atlasDataDAO;
    }

    private Map<String,QueryHandler> handlersMap;
    private final Map<String,QueryHandler> getHandlersMap() {
        if (handlersMap == null) {
            handlersMap = new TreeMap<String,QueryHandler>();
            handlersMap.put("experiments", new ExperimentsQueryHandler(experimentSolrDAO));
            handlersMap.put("assays", new AssaysQueryHandler(atlasDAO));
            handlersMap.put("data", new DataQueryHandler(bioEntityDAO, geneSolrDAO, atlasDataDAO, atlasDAO));
            handlersMap.put("genes", new GenesQueryHandler(geneSolrDAO));
        }
        return handlersMap;
    }

    private Error getUnsupportedPathError(String pathInfo) {
        final StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Unsupported path: \"");
        errorMessage.append(pathInfo);
        errorMessage.append("\"; Supported paths are: ");
        boolean first = true;
        for (String path : getHandlersMap().keySet()) {
            if (first) {
                first = false;
            } else {
                errorMessage.append(", ");
            }
            errorMessage.append("\"");
            errorMessage.append(path);
            errorMessage.append("\"");
        }
        errorMessage.append(".");
        return new Error(errorMessage.toString());
    }

    public void handleRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
        httpResponse.setContentType("application/json");
        httpResponse.setCharacterEncoding("utf-8");

        Request request = null;
        Object response;
        if (!"POST".equals(httpRequest.getMethod())) {
            response = new Error("Method " + httpRequest.getMethod() + " is not supported");
        } else {
            final String pathInfo = httpRequest.getPathInfo().replaceAll("/", " ").trim();
            final QueryHandler handler = getHandlersMap().get(pathInfo);
            if (handler == null) {
                response = getUnsupportedPathError(pathInfo);
            } else {
                try {
                    request = new ObjectMapper().readValue(httpRequest.getReader(), Request.class);
                    if (request.query != null) {
                        response = handler.getResponse(request.query);
                    } else {
                        response = new Error("Empty query");
                    }
                } catch (IOException e) {
                    log.error("Request parsing failed", e);
                    response = new Error(e.toString());
                }
            }
        }

        try {
            //final JsonRestResultRenderer renderer = new JsonRestResultRenderer(indent, 4, jsonCallback);
            final JsonRestResultRenderer renderer = new JsonRestResultRenderer(true, 4, null);
            //renderer.setErrorWrapper(ERROR_WRAPPER);
            FieldFilter filter = null;
            if (request != null && !(response instanceof Error)) {
                filter = MapBasedFieldFilter.createFilter(request.filter);
            }
            renderer.render(response, httpResponse.getWriter(), Object.class, filter);
        } catch (RestResultRenderException e) {
            log.error(e.getMessage());
        }
    }

    public void destroy() throws Exception {
//        if (indexBuilder != null)
//            indexBuilder.unregisterIndexBuildEventHandler(this);
    }
}

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
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
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

//import ae3.dao.NetCDFReader;
//import ae3.model.AtlasExperiment;
//import ae3.model.AtlasGene;
//import ae3.model.ExperimentalData;
//import ae3.service.AtlasStatisticsQueryService;
//import ae3.service.experiment.AtlasExperimentAnalyticsViewService;
//import ae3.service.experiment.AtlasExperimentQuery;
//import ae3.service.experiment.AtlasExperimentQueryParser;
//import ae3.service.structuredquery.*;
//import com.google.common.base.Function;
//import com.google.common.base.Predicate;
//import uk.ac.ebi.gxa.efo.Efo;
//import uk.ac.ebi.gxa.index.builder.IndexBuilder;
//import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
//import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
//import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
//import uk.ac.ebi.gxa.properties.AtlasProperties;
//import uk.ac.ebi.gxa.requesthandlers.api.result.*;
//import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;
//import uk.ac.ebi.gxa.utils.Pair;
//import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
//
//import javax.annotation.Nonnull;
//
//import static com.google.common.base.Predicates.alwaysTrue;
//import static com.google.common.base.Predicates.or;
//import static com.google.common.base.Strings.emptyToNull;
//import static com.google.common.base.Strings.isNullOrEmpty;
//import static com.google.common.collect.Collections2.transform;
//import static uk.ac.ebi.gxa.netcdf.reader.NetCDFPredicates.containsGenes;
//import static uk.ac.ebi.gxa.netcdf.reader.NetCDFPredicates.hasArrayDesign;

/**
 * REST API structured query servlet. Handles all gene and experiment API queries according to HTTP request parameters
 */
public class QueryRequestHandler implements HttpRequestHandler, /*IndexBuilderEventHandler,*/ DisposableBean {
    private Logger log = LoggerFactory.getLogger(getClass());

//    private AtlasStructuredQueryService queryService;
//    private AtlasProperties atlasProperties;
    private ExperimentSolrDAO experimentSolrDAO;
    private GeneSolrDAO geneSolrDAO;
    private AtlasDAO atlasDAO;
    private AtlasNetCDFDAO atlasNetCDFDAO;
//    private Efo efo;
//    private IndexBuilder indexBuilder;
//    private AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService;
//    private AtlasStatisticsQueryService atlasStatisticsQueryService;
//
//    volatile boolean disableQueries = false;
//
//    public void setQueryService(AtlasStructuredQueryService queryService) {
//        this.queryService = queryService;
//    }
//
    public void setExperimentSolrDAO(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }
//
//    public void setEfo(Efo efo) {
//        this.efo = efo;
//    }
//
//    public void setAtlasProperties(AtlasProperties atlasProperties) {
//        this.atlasProperties = atlasProperties;
//    }
//
//    public void setIndexBuilder(IndexBuilder builder) {
//        this.indexBuilder = builder;
//        builder.registerIndexBuildEventHandler(this);
//    }
//
//    public void setAtlasExperimentAnalyticsViewService(AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService) {
//        this.atlasExperimentAnalyticsViewService = atlasExperimentAnalyticsViewService;
//    }
//
//    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
//        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
//    }
//

    private Map<String,QueryHandler> handlersMap;
    private final Map<String,QueryHandler> getHandlersMap() {
        if (handlersMap == null) {
            handlersMap = new TreeMap<String,QueryHandler>();
            handlersMap.put("experiments", new ExperimentsQueryHandler(experimentSolrDAO));
            handlersMap.put("assays", new AssaysQueryHandler(atlasDAO));
            handlersMap.put("data", new DataQueryHandler(geneSolrDAO, atlasNetCDFDAO));
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

    private Object process(HttpServletRequest request) {
        final Request r;
        try {
            r = new ObjectMapper().readValue(request.getReader(), Request.class);
            log.info(r.toString());
        } catch (IOException e) {
            // TODO: envelop error message into standard API format
            return e.toString();
        }
        final ExperimentSolrDAO.AtlasExperimentsResult experiments = experimentSolrDAO.getExperimentsByQuery(r.toString(), 0, 200);
        return experiments;
        
//        if (disableQueries)
//            return new ErrorResult("API is temporarily unavailable, index building is in progress");
//
//        AtlasExperimentQuery query = AtlasExperimentQueryParser.parse(request, queryService.getEfvService().getAllFactors());
//        if (!query.isEmpty()) {
//            log.info("Experiment query: " + query.toSolrQuery());
//            final ExperimentSolrDAO.AtlasExperimentsResult experiments = experimentSolrDAO.getExperimentsByQuery(query.toSolrQuery(), query.getStart(), query.getRows());
//            if (experiments.getTotalResults() == 0)
//                return new ErrorResult("No such experiments found for: " + query);
//
//            final String arrayDesignAccession = emptyToNull(request.getParameter("hasArrayDesign"));
//            final QueryResultSortOrder queryResultSortOrder = request.getParameter("sort") == null ? QueryResultSortOrder.PVALUE : QueryResultSortOrder.valueOf(request.getParameter("sort"));
//            final int queryStart = query.getStart();
//            final int queryRows = query.getRows();
//
//            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(
//                    request, queryService.getGenePropertyOptions(), queryService.getEfvService().getAllFactors());
//
//            final Collection<ExpFactorQueryCondition> conditions = atlasQuery.getConditions();
//
//
//            final boolean experimentInfoOnly = (request.getParameter("experimentInfoOnly") != null);
//            final boolean experimentAnalytics = (request.getParameter("experimentAnalytics") != null);
//            final boolean experimentPageHeaderData = (request.getParameter("experimentPageHeader") != null);
//            final boolean experimentPageData = (request.getParameter("experimentPage") != null);
//
//            String upDownParam = request.getParameter("updown");
//            final QueryExpression statFilter = upDownParam == null ? QueryExpression.ANY :
//                    QueryExpression.parseFuzzyString(upDownParam);
//
//            Predicate<NetCDFProxy> genePredicate = alwaysTrue();
//
//            final Set<AtlasGene> genes = new HashSet<AtlasGene>();
//            if (!experimentInfoOnly && !experimentPageHeaderData) {
//                final String[] requestedGenes = request.getParameterValues("geneIs");
//                genes.addAll(getGeneIds(requestedGenes, atlasQuery));
//                if (requestedGenes != null && requestedGenes.length > 0) {
//                    genePredicate = or(transform(genes, new Function<AtlasGene, Predicate<? super NetCDFProxy>>() {
//                        public Predicate<? super NetCDFProxy> apply(@Nonnull AtlasGene input) {
//                            return containsGenes(Arrays.asList(input.getGeneId()));
//                        }
//                    }));
//                }
//            }
//
//            setRestProfile(experimentInfoOnly ? ExperimentRestProfile.class : ExperimentFullRestProfile.class);
//
//            if (experimentAnalytics)
//                setRestProfile(ExperimentAnalyticsRestProfile.class);
//            else if (experimentPageHeaderData)
//                setRestProfile(ExperimentPageHeaderRestProfile.class);
//            else if (experimentPageData)
//                setRestProfile(ExperimentPageRestProfile.class);
//
//            final Predicate<NetCDFProxy> netCDFProxyPredicate = !isNullOrEmpty(arrayDesignAccession) ?
//                    hasArrayDesign(arrayDesignAccession) : genePredicate;
//
//            return new ApiQueryResults<ExperimentResultAdapter>() {
//                public long getTotalResults() {
//                    return experiments.getTotalResults();
//                }
//
//                public long getNumberOfResults() {
//                    return experiments.getNumberOfResults();
//                }
//
//                public long getStartingFrom() {
//                    return experiments.getStartingFrom();
//                }
//
//                public Iterator<ExperimentResultAdapter> getResults() {
//                    return transform(experiments.getExperiments(),
//                            new Function<AtlasExperiment, ExperimentResultAdapter>() {
//                                public ExperimentResultAdapter apply(@Nonnull AtlasExperiment experiment) {
//                                    NetCDFDescriptor pathToNetCDFProxy = atlasNetCDFDAO.getNetCdfFile(experiment.getAccession(), netCDFProxyPredicate);
//
//                                    List<String> bestDesignElementIndexes = new ArrayList<String>();
//                                    List<AtlasGene> genesToPlot = new ArrayList<AtlasGene>();
//                                    ExperimentalData expData = null;
//                                    List<Pair<AtlasGene, ExpressionAnalysis>> geneResults = null;
//                                    if (!experimentInfoOnly && !experimentPageHeaderData) {
//                                        geneResults =
//                                                atlasExperimentAnalyticsViewService.findGenesForExperiment(
//                                                        experiment,
//                                                        genes,
//                                                        pathToNetCDFProxy,
//                                                        conditions,
//                                                        statFilter,
//                                                        queryResultSortOrder,
//                                                        queryStart,
//                                                        queryRows);
//
//                                        for (Pair<AtlasGene, ExpressionAnalysis> geneResult : geneResults) {
//                                            genesToPlot.add(geneResult.getFirst());
//                                            bestDesignElementIndexes.add(String.valueOf(geneResult.getSecond().getDesignElementIndex()));
//                                        }
//                                    }
//
//                                    if (!experimentInfoOnly) {
//                                        try {
//                                            expData = NetCDFReader.loadExperiment(atlasNetCDFDAO, experiment.getAccession());
//                                        } catch (IOException e) {
//                                            throw createUnexpected("Failed to read experimental data", e);
//                                        }
//                                    }
//                                    return new ExperimentResultAdapter(experiment, genesToPlot, geneResults, bestDesignElementIndexes, expData, atlasDAO, pathToNetCDFProxy, atlasProperties);
//                                }
//                            }).iterator();
//                }
//            };
//            //Heatmap page
//        } else {
//            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(
//                    request, queryService.getGenePropertyOptions(), queryService.getEfvService().getAllFactors());
//
//            if (!atlasQuery.isNone()) {
//                atlasQuery.setFullHeatmap(true);
//                atlasQuery.setViewType(ViewType.HEATMAP);
//                atlasQuery.setExpandColumns(queryService.getEfvService().getAllFactors());
//
//                AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
//                return new HeatmapResultAdapter(atlasResult, atlasDAO, efo, atlasProperties, atlasStatisticsQueryService);
//            } else {
//                return new ErrorResult("Empty query specified");
//            }
//        }
    }
//
//    public void onIndexBuildFinish() {
//        disableQueries = false;
//    }
//
//    public void onIndexBuildStart() {
//        disableQueries = true;
//    }
//
    public void destroy() throws Exception {
//        if (indexBuilder != null)
//            indexBuilder.unregisterIndexBuildEventHandler(this);
    }
//
//    /**
//     * @param geneIdsArr gene identifiers in user's query (if any)
//     * @param atlasQuery Structured query to retrieve genes by if none were provided in user's query
//     * @return the list of genes we think user has asked for
//     */
//    private Set<AtlasGene> getGeneIds(String[] geneIdsArr, AtlasStructuredQuery atlasQuery) {
//        Set<AtlasGene> genes = new HashSet<AtlasGene>();
//        // Attempt to find genes explicitly mentioned in the query; otherwise try to find
//        // them in Solr using any other search criteria provided
//        // TODO NB: currently we don't cater for gene=topX queries - 10 results are always returned if no genes have been explicitly specified
//        if (geneIdsArr != null && (geneIdsArr.length > 1 || !geneIdsArr[0].startsWith("top"))) {
//            // At least one gene was explicitly specified in the API query
//            for (String geneId : geneIdsArr) {
//                ExperimentSolrDAO.AtlasGeneResult agr = experimentSolrDAO.getGeneByIdentifier(geneId);
//                if (!agr.isFound()) {
//                    // If gene was not found by identifier, try to find it by its name
//                    for (AtlasGene gene : experimentSolrDAO.getGenesByName(geneId)) {
//                        if (!genes.contains(gene))
//                            genes.add(gene);
//                    }
//                } else if (!genes.contains(agr.getGene()))
//                    genes.add(agr.getGene());
//            }
//        } else { // No genes explicitly specified in the query - attempt to find them by any other search criteria
//            if (!atlasQuery.isNone() && 0 != atlasQuery.getGeneConditions().size()) {
//                atlasQuery.setFullHeatmap(false);
//                atlasQuery.setViewType(ViewType.HEATMAP);
//                atlasQuery.setConditions(Collections.<ExpFactorQueryCondition>emptyList());
//
//                AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
//                for (StructuredResultRow row : atlasResult.getResults()) {
//                    AtlasGene gene = row.getGene();
//                    genes.add(gene);
//                }
//            }
//        }
//        return genes;
//    }
}

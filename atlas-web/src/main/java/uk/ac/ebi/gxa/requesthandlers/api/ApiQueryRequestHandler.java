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

package uk.ac.ebi.gxa.requesthandlers.api;

import ae3.dao.ExperimentSolrDAO;
import ae3.dao.GeneSolrDAO;
import ae3.dao.NetCDFReader;
import ae3.model.AtlasGene;
import ae3.model.ExperimentalData;
import ae3.service.AtlasStatisticsQueryService;
import ae3.service.experiment.AtlasExperimentAnalyticsViewService;
import ae3.service.experiment.AtlasExperimentQuery;
import ae3.service.experiment.AtlasExperimentQueryParser;
import ae3.service.experiment.BestDesignElementsResult;
import ae3.service.structuredquery.*;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.Model;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.api.result.*;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Collections2.transform;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;
import static uk.ac.ebi.gxa.netcdf.reader.NetCDFPredicates.containsAtLeastOneGene;
import static uk.ac.ebi.gxa.netcdf.reader.NetCDFPredicates.hasArrayDesign;

/**
 * REST API structured query servlet. Handles all gene and experiment API queries according to HTTP request parameters
 *
 * @author pashky
 */
public class ApiQueryRequestHandler extends AbstractRestRequestHandler implements IndexBuilderEventHandler, DisposableBean {
    private AtlasStructuredQueryService queryService;
    private AtlasProperties atlasProperties;
    private GeneSolrDAO geneSolrDAO;
    private ExperimentSolrDAO experimentSolrDAO;
    private Model atlasModel;
    private AtlasDAO atlasDAO;
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private IndexBuilder indexBuilder;
    private AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;

    volatile boolean disableQueries = false;

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    public void setExperimentSolrDAO(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAtlasModel(Model atlasModel) {
        this.atlasModel = atlasModel;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setIndexBuilder(IndexBuilder builder) {
        this.indexBuilder = builder;
        builder.registerIndexBuildEventHandler(this);
    }

    public void setAtlasExperimentAnalyticsViewService(AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService) {
        this.atlasExperimentAnalyticsViewService = atlasExperimentAnalyticsViewService;
    }

    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    @Override
    public Object process(HttpServletRequest request) {
        if (disableQueries)
            return new ErrorResult("API is temporarily unavailable, index building is in progress");

        AtlasExperimentQuery query = AtlasExperimentQueryParser.parse(request, queryService.getAllFactors());
        if (!query.isEmpty()) {
            log.info("Experiment query: " + query.toSolrQuery());
            final ExperimentSolrDAO.AtlasExperimentsResult experiments = experimentSolrDAO.getExperimentsByQuery(query.toSolrQuery(), query.getStart(), query.getRows());
            if (experiments.getTotalResults() == 0)
                return new ErrorResult("No such experiments found for: " + query);

            final String arrayDesignAccession = emptyToNull(request.getParameter("hasArrayDesign"));

            String s = request.getParameter("sort");
            final QueryResultSortOrder queryResultSortOrder = s == null ? QueryResultSortOrder.PVALUE : QueryResultSortOrder.valueOf(s);

            s = request.getParameter("offset");
            final int queryStart = s == null ? 0 : Integer.parseInt(s);

            s = request.getParameter("limit");
            final int queryRows = s == null ? 10 : Integer.parseInt(s);

            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(
                    request, queryService.getGenePropertyOptions(), queryService.getAllFactors());

            final Collection<ExpFactorQueryCondition> conditions = atlasQuery.getConditions();


            final boolean experimentInfoOnly = (request.getParameter("experimentInfoOnly") != null);
            final boolean experimentAnalytics = (request.getParameter("experimentAnalytics") != null);
            final boolean experimentPageData = (request.getParameter("experimentPage") != null);

            String upDownParam = request.getParameter("updown");
            final QueryExpression statFilter = upDownParam == null ? QueryExpression.ANY :
                    QueryExpression.parseFuzzyString(upDownParam);

            Predicate<NetCDFProxy> genePredicate = alwaysTrue();

            final Set<Long> geneIds = new HashSet<Long>();
            if (!experimentInfoOnly) {
                final String[] requestedGeneIds = request.getParameterValues("geneIs");
                if (requestedGeneIds != null && requestedGeneIds.length > 0) {
                    geneIds.addAll(getGenes(requestedGeneIds, atlasQuery));
                    genePredicate = containsAtLeastOneGene(geneIds);
                }
            }

            setRestProfile(experimentInfoOnly ? ExperimentRestProfile.class : ExperimentFullRestProfile.class);

            if (experimentAnalytics)
                setRestProfile(ExperimentAnalyticsRestProfile.class);
            else if (experimentPageData)
                setRestProfile(ExperimentPageRestProfile.class);

            final Predicate<NetCDFProxy> netCDFProxyPredicate = !isNullOrEmpty(arrayDesignAccession) ?
                    hasArrayDesign(arrayDesignAccession) : genePredicate;

            return new ApiQueryResults<ExperimentResultAdapter>() {
                public long getTotalResults() {
                    return experiments.getTotalResults();
                }

                public long getNumberOfResults() {
                    return experiments.getNumberOfResults();
                }

                public long getStartingFrom() {
                    return experiments.getStartingFrom();
                }

                public Iterator<ExperimentResultAdapter> getResults() {
                    return transform(experiments.getExperiments(),
                            new Function<Experiment, ExperimentResultAdapter>() {
                                public ExperimentResultAdapter apply(@Nonnull Experiment experiment) {
                                    NetCDFDescriptor pathToNetCDFProxy = atlasNetCDFDAO.getNetCdfFile(experiment.getAccession(), netCDFProxyPredicate);

                                    ExperimentalData expData = null;
                                    BestDesignElementsResult geneResults = null;
                                    if (!experimentInfoOnly) {
                                        geneResults =
                                                atlasExperimentAnalyticsViewService.findBestGenesForExperiment(
                                                        experiment,
                                                        geneIds,
                                                        pathToNetCDFProxy,
                                                        conditions,
                                                        statFilter,
                                                        queryResultSortOrder,
                                                        queryStart,
                                                        queryRows);
                                    }

                                    if (!experimentInfoOnly) {
                                        try {
                                            expData = NetCDFReader.loadExperiment(atlasNetCDFDAO, experiment.getAccession());
                                        } catch (IOException e) {
                                            throw createUnexpected("Failed to read experimental data", e);
                                        }
                                    }
                                    return new ExperimentResultAdapter(experiment, geneResults, expData, atlasDAO, pathToNetCDFProxy, atlasProperties);
                                }
                            }).iterator();
                }
            };
            //Heatmap page
        } else {
            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(
                    request, queryService.getGenePropertyOptions(), queryService.getAllFactors());

            if (!atlasQuery.isNone()) {
                atlasQuery.setFullHeatmap(true);
                atlasQuery.setViewType(ViewType.HEATMAP);
                atlasQuery.setExpandColumns(queryService.getAllFactors());

                AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
                if (atlasResult.getUserErrorMsg() != null) {
                    return new ErrorResult(atlasResult.getUserErrorMsg());
                }
                return new HeatmapResultAdapter(atlasResult, atlasModel, atlasProperties, atlasStatisticsQueryService);
            } else {
                return new ErrorResult("Empty query specified");
            }
        }
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

    /**
     * @param geneIdsArr gene identifiers in user's query (if any)
     * @param atlasQuery Structured query to retrieve genes by if none were provided in user's query
     * @return the list of genes we think user has asked for
     */
    private Set<Long> getGenes(String[] geneIdsArr, AtlasStructuredQuery atlasQuery) {
        Set<Long> genes = new HashSet<Long>();
        // Attempt to find genes explicitly mentioned in the query; otherwise try to find
        // them in Solr using any other search criteria provided
        // TODO NB: currently we don't cater for gene=topX queries - 10 results are always returned if no genes have been explicitly specified
        if (geneIdsArr != null && (geneIdsArr.length > 1 || !geneIdsArr[0].startsWith("top"))) {
            // At least one gene was explicitly specified in the API query
            for (String geneId : geneIdsArr) {
                GeneSolrDAO.AtlasGeneResult agr = geneSolrDAO.getGeneByIdentifier(geneId);
                if (!agr.isFound()) {
                    // If gene was not found by identifier, try to find it by its name
                    for (AtlasGene gene : geneSolrDAO.getGenesByName(geneId)) {
                            genes.add((long) gene.getGeneId());
                    }
                } else {
                    genes.add((long) agr.getGene().getGeneId());
                }
            }
        } else { // No genes explicitly specified in the query - attempt to find them by any other search criteria
            if (!atlasQuery.isNone() && 0 != atlasQuery.getGeneConditions().size()) {
                atlasQuery.setFullHeatmap(false);
                atlasQuery.setViewType(ViewType.HEATMAP);
                atlasQuery.setConditions(Collections.<ExpFactorQueryCondition>emptyList());

                AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
                for (StructuredResultRow row : atlasResult.getResults()) {
                    AtlasGene gene = row.getGene();
                    genes.add((long) gene.getGeneId());
                }
            }
        }
        return genes;
    }
}

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

import ae3.dao.AtlasSolrDAO;
import ae3.dao.NetCDFReader;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ExperimentalData;
import ae3.model.ListResultRow;
import ae3.service.experiment.AtlasExperimentAnalyticsViewService;
import ae3.service.experiment.AtlasExperimentQuery;
import ae3.service.experiment.AtlasExperimentQueryParser;
import ae3.service.structuredquery.*;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.gxa.requesthandlers.api.result.*;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;

import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

/**
 * REST API structured query servlet. Handles all gene and experiment API queries according to HTTP request parameters
 *
 * @author pashky
 */
public class ApiQueryRequestHandler extends AbstractRestRequestHandler implements IndexBuilderEventHandler, DisposableBean {
    private AtlasStructuredQueryService queryService;
    private AtlasProperties atlasProperties;
    private AtlasSolrDAO atlasSolrDAO;
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private Efo efo;
    private IndexBuilder indexBuilder;

    private String netCDFPath;
    boolean disableQueries = false;
    private AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService;

    public AtlasStructuredQueryService getQueryService() {
        return queryService;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public AtlasSolrDAO getDao() {
        return atlasSolrDAO;
    }

    public void setDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public Efo getEfo() {
        return efo;
    }

    public void setEfo(Efo efo) {
        this.efo = efo;
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

    public void setIndexBuilder(IndexBuilder builder) {
        this.indexBuilder = builder;
        builder.registerIndexBuildEventHandler(this);
    }

    public void setNetCDFPath(File netCDFPath) {
        this.netCDFPath = netCDFPath.getAbsolutePath();
    }

    @Override
    public Object process(HttpServletRequest request) {
        if (disableQueries)
            return new ErrorResult("API is temporarily unavailable, index building is in progress");

        AtlasExperimentQuery query = AtlasExperimentQueryParser.parse(request, queryService.getEfvService().getAllFactors());
        if (!query.isEmpty()) {
            log.info("Experiment query: " + query.toSolrQuery());
            final AtlasSolrDAO.AtlasExperimentsResult experiments = atlasSolrDAO.getExperimentsByQuery(query.toSolrQuery(), query.getStart(), query.getRows());
            if (experiments.getTotalResults() == 0)
                return new ErrorResult("No such experiments found for: " + query);

            final String arrayDesignAccession = request.getParameter("hasArrayDesign");
            final QueryResultSortOrder queryResultSortOrder = request.getParameter("sort") == null ? QueryResultSortOrder.PVALUE : QueryResultSortOrder.valueOf(request.getParameter("sort"));
            final int queryStart = query.getStart();
            final int queryRows = query.getRows();

            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(
                    request, queryService.getGenePropertyOptions(), queryService.getEfvService().getAllFactors());

            final Collection<ExpFactorQueryCondition> conditions = atlasQuery.getConditions();

            // Order of genes is important when API for ordering of plotted genes on the experiment page - hence LinkedHashSet()
            final Set<AtlasGene> genes = new HashSet<AtlasGene>();
            final Set<Long> geneIds = new HashSet<Long>();
            if (!atlasQuery.isNone() && 0 != atlasQuery.getGeneConditions().size()) {
                atlasQuery.setFullHeatmap(false);
                atlasQuery.setViewType(ViewType.HEATMAP);
                atlasQuery.setConditions(Collections.<ExpFactorQueryCondition>emptyList());

                AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
                for(StructuredResultRow row : atlasResult.getResults()) {
                    AtlasGene gene = row.getGene();
                    genes.add(gene);
                    geneIds.add(Long.parseLong(gene.getGeneId()));
                }

                if(genes.isEmpty())
                    return new ErrorResult("No genes found for specified query");
            }

            final boolean experimentInfoOnly = (request.getParameter("experimentInfoOnly") != null);
            final boolean experimentAnalytics = (request.getParameter("experimentAnalytics") != null);
            final boolean experimentPageHeaderData = (request.getParameter("experimentPageHeader") != null);
            final boolean experimentPageData = (request.getParameter("experimentPage") != null);


            setRestProfile(experimentInfoOnly ? ExperimentRestProfile.class : ExperimentFullRestProfile.class);

            if (experimentAnalytics)
                setRestProfile(ExperimentAnalyticsRestProfile.class);
            else if (experimentPageHeaderData)
                setRestProfile(ExperimentPageHeaderRestProfile.class);
            else if (experimentPageData)
                setRestProfile(ExperimentPageRestProfile.class);

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
                    return new MappingIterator<AtlasExperiment, ExperimentResultAdapter>(experiments.getExperiments().iterator()) {
                        public ExperimentResultAdapter map(AtlasExperiment experiment) {
                            String pathToNetCDFProxy = null;
                            String proxyId = atlasNetCDFDAO.findProxyId(String.valueOf(experiment.getId()), arrayDesignAccession, geneIds);
                            if (proxyId != null) {
                                pathToNetCDFProxy = netCDFPath + File.separator + proxyId;
                            }

                            List<Pair<AtlasGene, ExpressionAnalysis>> geneResults = null;
                            List<String> bestDesignElementIndexes = new ArrayList<String>();
                            List<AtlasGene> genesToPlot = new ArrayList<AtlasGene>();
                            if (experimentAnalytics || experimentPageData) {
                                geneResults = atlasExperimentAnalyticsViewService.findGenesForExperiment(
                                        experiment,
                                        genes,
                                        pathToNetCDFProxy,
                                        conditions,
                                        queryResultSortOrder,
                                        queryStart,
                                        queryRows);

                                for (Pair<AtlasGene, ExpressionAnalysis> geneResult : geneResults) {
                                    genesToPlot.add(geneResult.getFirst());
                                    bestDesignElementIndexes.add(String.valueOf(geneResult.getSecond().getDesignElementIndex()));
                                }
                            }
                            ExperimentalData expData = null;
                            if(!experimentInfoOnly) {
                                try {
                                    expData = NetCDFReader.loadExperiment(netCDFPath, experiment.getId());
                                } catch (IOException e) {
                                    throw new RuntimeException("Failed to read experimental data");
                                }
                            }
                            return new ExperimentResultAdapter(experiment, genesToPlot, geneResults, bestDesignElementIndexes, expData, atlasSolrDAO, pathToNetCDFProxy, atlasProperties);
                        }
                    };
                }
            };
            //Heatmap page
        } else {
            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(
                    request, queryService.getGenePropertyOptions(), queryService.getEfvService().getAllFactors());

            if (!atlasQuery.isNone()) {
                atlasQuery.setFullHeatmap(true);
                atlasQuery.setViewType(ViewType.HEATMAP);
		        atlasQuery.setExpandColumns(queryService.getEfvService().getAllFactors());

                AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
                return new HeatmapResultAdapter(atlasResult, atlasSolrDAO, efo, atlasProperties);
            } else {
                return new ErrorResult("Empty query specified");
            }
        }
    }

    public void destroy() throws Exception {
        if (indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }

    public void setAtlasExperimentAnalyticsViewService(AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService) {
        this.atlasExperimentAnalyticsViewService = atlasExperimentAnalyticsViewService;
    }

    public AtlasExperimentAnalyticsViewService getAtlasExperimentAnalyticsViewService() {
        return atlasExperimentAnalyticsViewService;
    }
}

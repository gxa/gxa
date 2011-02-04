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
import ae3.service.AtlasStatisticsQueryService;
import ae3.service.experiment.AtlasExperimentAnalyticsViewService;
import ae3.service.experiment.AtlasExperimentQuery;
import ae3.service.experiment.AtlasExperimentQueryParser;
import ae3.service.structuredquery.*;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.api.result.*;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * REST API structured query servlet. Handles all gene and experiment API queries according to HTTP request parameters
 *
 * @author pashky
 */
public class ApiQueryRequestHandler extends AbstractRestRequestHandler implements IndexBuilderEventHandler, DisposableBean {
    private AtlasStructuredQueryService queryService;
    private AtlasProperties atlasProperties;
    private AtlasSolrDAO atlasSolrDAO;
    private AtlasDAO atlasDAO;
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private Efo efo;
    private IndexBuilder indexBuilder;
    private AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService;
        private AtlasStatisticsQueryService atlasStatisticsQueryService;

    volatile boolean disableQueries = false;

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public void setDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void setEfo(Efo efo) {
        this.efo = efo;
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

            final Set<AtlasGene> genes = new HashSet<AtlasGene>();
            final Set<Long> geneIds = new HashSet<Long>();

            final boolean experimentInfoOnly = (request.getParameter("experimentInfoOnly") != null);
            final boolean experimentAnalytics = (request.getParameter("experimentAnalytics") != null);
            final boolean experimentPageHeaderData = (request.getParameter("experimentPageHeader") != null);
            final boolean experimentPageData = (request.getParameter("experimentPage") != null);

            if (!experimentInfoOnly && !experimentPageHeaderData) {
                genes.addAll(getGeneIds(request.getParameterValues("geneIs"), atlasQuery));
                for (AtlasGene gene : genes) {
                    geneIds.add(Long.parseLong(gene.getGeneId()));
                }
            }

            setRestProfile(experimentInfoOnly ? ExperimentRestProfile.class : ExperimentFullRestProfile.class);

            if (experimentAnalytics)
                setRestProfile(ExperimentAnalyticsRestProfile.class);
            else if (experimentPageHeaderData)
                setRestProfile(ExperimentPageHeaderRestProfile.class);
            else if (experimentPageData)
                setRestProfile(ExperimentPageRestProfile.class);

            final String lastKnownReleaseDate_String = request.getParameter("lastKnownReleaseDate");
            /* not final*/ Date tmp_Date = null;
             if(null!=lastKnownReleaseDate_String)
                try{
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    tmp_Date = (Date)formatter.parse(lastKnownReleaseDate_String);
                }catch(ParseException e){
                    tmp_Date = null;
                }
            
            final Date lastKnownReleaseDate = tmp_Date;

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
                    return Collections2.transform(experiments.getExperiments(),
                            new Function<AtlasExperiment, ExperimentResultAdapter>() {
                                public ExperimentResultAdapter apply(@Nullable AtlasExperiment experiment) {
                                    NetCDFDescriptor pathToNetCDFProxy = atlasNetCDFDAO.getNetCdfFile(experiment.getAccession(), arrayDesignAccession, geneIds);

                                    List<String> bestDesignElementIndexes = new ArrayList<String>();
                                    List<AtlasGene> genesToPlot = new ArrayList<AtlasGene>();
                                    ExperimentalData expData = null;
                                    List<Pair<AtlasGene, ExpressionAnalysis>> geneResults = null;
                                    if (!experimentInfoOnly && !experimentPageHeaderData) {
                                        geneResults =
                                                atlasExperimentAnalyticsViewService.findGenesForExperiment(
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

                                    if (!experimentInfoOnly) {
                                        try {
                                            expData = NetCDFReader.loadExperiment(atlasNetCDFDAO, experiment.getAccession());
                                        } catch (IOException e) {
                                            throw new RuntimeException("Failed to read experimental data");
                                        }
                                    }
                                    return new ExperimentResultAdapter(experiment, genesToPlot, geneResults, bestDesignElementIndexes, expData, atlasSolrDAO, pathToNetCDFProxy, atlasProperties);
                                }
                            }).iterator();
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
                return new HeatmapResultAdapter(atlasResult, atlasDAO, efo, atlasProperties, atlasStatisticsQueryService);
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
     *
     * @param geneIdsArr gene identifiers in user's query (if any)
     * @param atlasQuery Structured query to retrieve genes by if none were provided in user's query
     * @return
     */
    private Set<AtlasGene> getGeneIds(String[] geneIdsArr, AtlasStructuredQuery atlasQuery) {
        Set<AtlasGene> genes = new HashSet<AtlasGene>();
        // Attempt to find genes explicitly mentioned in the query; otherwise try to find
        // them in Solr using any other search criteria provided
        // TODO NB: currently we don't cater for gene=topX queries - 10 results are always returned if no genes have been explicitly specified
        if (geneIdsArr != null && (geneIdsArr.length > 1 || !geneIdsArr[0].startsWith("top"))) {
            // At least one gene was explicitly specified in the API query
            for (String geneId : geneIdsArr) {
                AtlasSolrDAO.AtlasGeneResult agr = atlasSolrDAO.getGeneByIdentifier(geneId);
                if (!agr.isFound()) {
                    // If gene was not found by identifier, try to find it by its name
                    for (AtlasGene gene : atlasSolrDAO.getGenesByName(geneId)) {
                        if (!genes.contains(gene))
                            genes.add(gene);
                    }
                } else if (!genes.contains(agr.getGene()))
                    genes.add(agr.getGene());
            }
        } else { // No genes explicitly specified in the query - attempt to find them by any other search criteria
            if (!atlasQuery.isNone() && 0 != atlasQuery.getGeneConditions().size()) {
                atlasQuery.setFullHeatmap(false);
                atlasQuery.setViewType(ViewType.HEATMAP);
                atlasQuery.setConditions(Collections.<ExpFactorQueryCondition>emptyList());

                AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
                for (StructuredResultRow row : atlasResult.getResults()) {
                    AtlasGene gene = row.getGene();
                    genes.add(gene);
                }
            }
        }
        return genes;
    }
}

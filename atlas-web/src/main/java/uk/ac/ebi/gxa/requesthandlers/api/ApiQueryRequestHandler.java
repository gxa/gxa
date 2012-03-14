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
import ae3.model.AtlasExperiment;
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
import com.google.common.base.Predicates;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.api.result.*;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.microarray.atlas.model.DesignElementStatistics.ANY_KNOWN_GENE;

/**
 * REST API structured query servlet. Handles all gene and experiment API queries according to HTTP request parameters
 *
 * @author pashky
 */
public class ApiQueryRequestHandler extends AbstractRestRequestHandler implements IndexBuilderEventHandler, DisposableBean {
    private AtlasStructuredQueryService queryService;
    private AtlasProperties atlasProperties;
    private ExperimentSolrDAO experimentSolrDAO;
    private GeneSolrDAO geneSolrDAO;
    private ExperimentDAO experimentDAO;
    private AtlasDataDAO atlasDataDAO;
    private IndexBuilder indexBuilder;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;
    private AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService;

    volatile boolean disableQueries = false;

    public void setExperimentDAO(ExperimentDAO experimentDAO) {
        this.experimentDAO = experimentDAO;
    }

    public void setGeneSolrDAO(GeneSolrDAO geneSolrDAO) {
        this.geneSolrDAO = geneSolrDAO;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public void setExperimentSolrDAO(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }

    public void setAtlasDataDAO(AtlasDataDAO atlasDataDAO) {
        this.atlasDataDAO = atlasDataDAO;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void setIndexBuilder(IndexBuilder builder) {
        this.indexBuilder = builder;
        builder.registerIndexBuildEventHandler(this);
    }

    public void setAtlasStatisticsQueryService(AtlasStatisticsQueryService atlasStatisticsQueryService) {
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
    }

    public void setAtlasExperimentAnalyticsViewService(AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService) {
        this.atlasExperimentAnalyticsViewService = atlasExperimentAnalyticsViewService;
    }

    private static class ExperimentResults implements ApiQueryResults<ExperimentResultAdapter> {
        private final ExperimentSolrDAO.AtlasExperimentsResult experiments;
        private final Collection<ExperimentResultAdapter> results;

        ExperimentResults(ExperimentSolrDAO.AtlasExperimentsResult experiments, Collection<ExperimentResultAdapter> results) {
            this.experiments = experiments;
            this.results = results;
        }

        public long getTotalResults() {
            return experiments.getTotalResults();
        }

        public long getNumberOfResults() {
            return experiments.getNumberOfResults();
        }

        public long getStartingFrom() {
            return experiments.getStartingFrom();
        }

        public Collection<ExperimentResultAdapter> getResults() {
            return results;
        }
    }

    @Override
    public Object process(HttpServletRequest request) {
        if (disableQueries)
            return new ErrorResult("API is temporarily unavailable, index building is in progress");

        final AtlasExperimentQuery query = new AtlasExperimentQueryParser(atlasProperties, queryService.getAllFactors()).parse((Map<String, String[]>) request.getParameterMap());
        if (query.isValid()) {
            final ExperimentSolrDAO.AtlasExperimentsResult experiments = experimentSolrDAO.getExperimentsByQuery(query);
            if (experiments.getTotalResults() == 0)
                return new ErrorResult("No such experiments found for: " + query);

            final boolean experimentInfoOnly = (request.getParameter("experimentInfoOnly") != null);
            final boolean experimentAnalytics = (request.getParameter("experimentAnalytics") != null);

            String s = request.getParameter("offset");
            final int genesStart = s == null ? 0 : Integer.parseInt(s);

            s = request.getParameter("limit");
            final int genesRows = s == null ? 10 : Integer.parseInt(s);

            setRestProfile(experimentInfoOnly ? ExperimentRestProfile.class : ExperimentFullRestProfile.class);

            if (experimentAnalytics) {
                setRestProfile(ExperimentAnalyticsRestProfile.class);
            }

            return new ExperimentResults(
                    experiments,
                    transform(experiments.getAtlasExperiments(),
                            new Function<AtlasExperiment, ExperimentResultAdapter>() {
                                public ExperimentResultAdapter apply(@Nonnull AtlasExperiment experiment) {
                                    Collection<AtlasGene> genes = Collections.emptyList();
                                    ExperimentalData expData = null;

                                    if (!experimentInfoOnly) {
                                        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(experiment.getExperiment());
                                        try {
                                            Collection<String> geneIdentifiers = query.getGeneIdentifiers();
                                            Predicate<Long> geneIdPredicate = geneIdentifiers.isEmpty() ?
                                                    ANY_KNOWN_GENE :
                                                    in(geneSolrDAO.findGeneIds(geneIdentifiers));

                                            ExperimentPartCriteria criteria = ExperimentPartCriteria.experimentPart();
                                            criteria.containsAtLeastOneGene(geneIdPredicate);

                                            //TODO: trac #2954 Ambiguous behaviour of getting top 10 genes in the experiment API call
                                            BestDesignElementsResult geneResults =
                                                    atlasExperimentAnalyticsViewService.findBestGenesForExperiment(
                                                            criteria.retrieveFrom(ewd),
                                                            geneIdPredicate,
                                                            UpDownCondition.CONDITION_ANY,
                                                            Predicates.<Pair<String, String>>alwaysTrue(),
                                                            genesStart, genesRows
                                                    );
                                            genes = geneResults.getGenes();
                                            expData = new ExperimentalData(ewd);
                                        } catch (AtlasDataException e) {
                                            log.warn("AtlasDataException thrown", e);
                                        } catch (StatisticsNotFoundException e) {
                                            log.warn("StatisticsNotFoundException thrown", e);
                                        } finally {
                                            closeQuietly(ewd);
                                        }
                                    }

                                    return new ExperimentResultAdapter(experiment, genes, expData);
                                }
                            })
            );
        } else {
            //Heatmap page
            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(
                    request, queryService.getGenePropertyOptions(), queryService.getAllFactors(), atlasProperties);
            if (atlasQuery.isNone()) {
                return new ErrorResult("Empty query specified");
            }

            atlasQuery.setFullHeatmap(true);
            atlasQuery.setViewType(ViewType.HEATMAP);
            atlasQuery.setExpandColumns(queryService.getAllFactors());

            AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
            if (atlasResult.getUserErrorMsg() != null) {
                return new ErrorResult(atlasResult.getUserErrorMsg());
            }
            return new HeatmapResultAdapter(atlasResult, experimentDAO, atlasProperties, atlasStatisticsQueryService);
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
}

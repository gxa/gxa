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

import ae3.dao.AtlasDao;
import ae3.dao.NetCDFReader;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ExperimentalData;
import ae3.model.ListResultRow;
import ae3.service.experiment.AtlasExperimentQuery;
import ae3.service.experiment.AtlasExperimentQueryParser;
import ae3.service.structuredquery.*;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;
import uk.ac.ebi.gxa.utils.MappingIterator;
import uk.ac.ebi.gxa.requesthandlers.api.result.*;
import uk.ac.ebi.gxa.requesthandlers.base.result.ErrorResult;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.DisposableBean;

/**
 * REST API structured query servlet. Handles all gene and experiment API queries according to HTTP request parameters
 *
 * @author pashky
 */
public class ApiQueryRequestHandler extends AbstractRestRequestHandler implements IndexBuilderEventHandler, DisposableBean {
    private AtlasStructuredQueryService queryService;
    private AtlasDao dao;
    private Efo efo;
    private IndexBuilder indexBuilder;

    private String netCDFPath;
    boolean disableQueries = false;

    public AtlasStructuredQueryService getQueryService() {
        return queryService;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public AtlasDao getDao() {
        return dao;
    }

    public void setDao(AtlasDao dao) {
        this.dao = dao;
    }

    public Efo getEfo() {
        return efo;
    }

    public void setEfo(Efo efo) {
        this.efo = efo;
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
        if(disableQueries)
            return new ErrorResult("API is temporarily unavailable, index building is in progress");

        AtlasExperimentQuery query = AtlasExperimentQueryParser.parse(request, queryService.getEfvService().getAllFactors());
        if(!query.isEmpty()) {
            log.info("Experiment query: " + query.toSolrQuery());
            final AtlasDao.AtlasExperimentsResult experiments = dao.getExperimentsByQuery(query.toSolrQuery(), query.getStart(), query.getRows());
            if(experiments.getTotalResults() == 0)
                return new ErrorResult("No such experiments found for: " + query);

            final List<AtlasGene> genes = new ArrayList<AtlasGene>();
            int nTop = 0;
            final String[] geneIds = request.getParameterValues("gene");
            if(geneIds != null) {
                if(geneIds.length == 1 && geneIds[0].startsWith("top")) {
                    try {
                        nTop = Integer.valueOf(geneIds[0].substring(3));
                        if(nTop > 100)
                            nTop = 10;
                    } catch(Exception e) {/**/}
                } else {
                    for(String geneId : geneIds) {
                        AtlasDao.AtlasGeneResult agr = dao.getGeneByIdentifier(geneId);
                        if(agr.isFound() && !genes.contains(agr.getGene()))
                            genes.add(agr.getGene());
                    }
                }
            }

            final int nTopFinal = nTop;

            setRestProfile(request.getParameter("experimentInfoOnly") != null ? ExperimentRestProfile.class : ExperimentFullRestProfile.class);

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
                            if(nTopFinal > 0) {
                                genes.clear();
                                for(ListResultRow r : queryService.findGenesForExperiment("", experiment.getId(), 0, nTopFinal))
                                    genes.add(r.getGene());
                            }

                            ExperimentalData expData = null;
                            try {
                                 expData = NetCDFReader.loadExperiment(netCDFPath, experiment.getId());
                            } catch(IOException e) {
                                throw new RuntimeException("Failed to read experimental data");
                            }
                            return new ExperimentResultAdapter(experiment, genes, expData, dao);
                        }
                    };
                }
            };
        } else {
            AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(
                    request, queryService.getGenePropertyOptions(), queryService.getEfvService().getAllFactors());

            if (!atlasQuery.isNone()) {
                atlasQuery.setFullHeatmap(true);
                atlasQuery.setViewType(ViewType.HEATMAP);
                AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);
                return new HeatmapResultAdapter(atlasResult, dao, efo);
            }
            else {
                return new ErrorResult("Empty query specified");
            }
        }
    }

    public void destroy() throws Exception {
        if(indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }
}

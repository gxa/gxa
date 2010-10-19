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

package uk.ac.ebi.gxa.requesthandlers.experimentpage;

import ae3.model.ListResultRow;
import ae3.service.structuredquery.*;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.requesthandlers.experimentpage.result.SimilarityResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.rcloud.server.RServices;
import uk.ac.ebi.rcloud.server.RType.RDataFrame;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class GeneListWidgetRequestHandler implements HttpRequestHandler {
    final private Logger log = LoggerFactory.getLogger(getClass());

    private AtlasStructuredQueryService queryService;
    private AtlasComputeService computeService;
    private AtlasProperties atlasProperties;
    private File atlasNetCDFRepo;

    public AtlasStructuredQueryService getQueryService() {
        return queryService;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public AtlasComputeService getComputeService() {
        return computeService;
    }

    public void setComputeService(AtlasComputeService computeService) {
        this.computeService = computeService;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        long eid = 1; //Long.valueOf(request.getParameter("eid"));
        String qryType = ""; //request.getParameter("query");
        String geneId = "1"; //request.getParameter("gid");
        String startRow = "1"; //request.getParameter("from");

        AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRequest(request, atlasProperties);

        atlasQuery.setViewType(ViewType.LIST);
        atlasQuery.setExpsPerGene(3);

            ////atlasQuery.setRowsPerPage(4);
            //atlasQuery.setExpandColumns(new HashSet<String>()); //omit?
            ////atlasQuery.setStart(0);
            //atlasQuery.setGeneConditions(new ArrayList<GeneQueryCondition>());
            ////atlasQuery.setFullHeatmap(false);
            ////atlasQuery.setSpecies(new ArrayList<String>()); //?

            ExpFactorQueryCondition condition = new ExpFactorQueryCondition();
            condition.setExpression(QueryExpression.UP_DOWN);
            condition.setMinExperiments(1);
            condition.setFactor("experiment");
            condition.setFactorValues(new ArrayList<String>(){{add("E-GEOD-5258");}});
            Collection<ExpFactorQueryCondition> conditions = new ArrayList<ExpFactorQueryCondition>();
            atlasQuery.getConditions().clear();
            atlasQuery.getConditions().add(condition);

            //atlasQuery.setConditions(conditions);

            AtlasStructuredQueryResult atlasResult = queryService.doStructuredAtlasQuery(atlasQuery);

        /*
        if(geneQuery != null) {
            SimilarityResultSet simRS = (SimilarityResultSet) request.getAttribute("simRS");
            String proxyId = null;
            if (simRS != null) {
                proxyId = simRS.getSourceNetCDF();
            }

            request.setAttribute("geneList", queryService.findGenesForExperiment(geneQuery, proxyId, eid, start,
                    atlasProperties.getQueryListSize()));
        }
        */


            //List<ListResultRow> genesForExperiment = queryService.findGenesForExperiment(geneQuery, eid, start,
            //        atlasProperties.getQueryListSize());

            request.setAttribute("result", atlasResult);


        request.setAttribute("eid", eid);
        request.setAttribute("gid", geneId);

        request.getRequestDispatcher("/WEB-INF/jsp/experimentpage/expression-table.jsp").forward(request, response);
    }

    //from               to
    //GeneA              GeneA
    //   condition1        condition1
    //   condition2      GeneA
    //GeneB                condition1
    //   condition3      GeneB
    //                     ...
    private AtlasStructuredQueryResult oneConditionPerGene(AtlasStructuredQueryResult atlasResult){

        Collection<ListResultRow> results = new ArrayList<ListResultRow>();

        for(ListResultRow row : atlasResult.getListResults()){
            //for(row.)
        }

        //atlasResult.setListResults(results);
        return atlasResult;
    }

    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }

    public File getAtlasNetCDFRepo() {
        return atlasNetCDFRepo;
    }

    private String getRCodeFromResource(String resourcePath) throws ComputeException {
        // open a stream to the resource
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);

        // create a reader to read in code
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        StringBuilder sb = new StringBuilder();
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new ComputeException("Error while reading in R code from " + resourcePath, e);
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("Failed to close input stream", e);
                }
            }
        }

        return sb.toString();
    }

}

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

package uk.ac.ebi.gxa.requesthandlers.genepage;

import ae3.dao.AtlasSolrDAO;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

import ae3.model.AtlasExperiment;

/**
 * Experiment list in gene page
 * @author pashky
 */
public class ExperimentsListRequestHandler implements HttpRequestHandler {
    private AtlasSolrDAO atlasSolrDAO;

    public AtlasSolrDAO getAtlasSolrDao() {
        return atlasSolrDAO;
    }

    public void setAtlasSolrDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }


    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String geneId = request.getParameter("gid");
        if (geneId != null) {
            int fromRow = -1;
            try { fromRow = Integer.valueOf(request.getParameter("from")); } catch (Exception e) { }
            int toRow = -1;
            try { toRow = Integer.valueOf(request.getParameter("to")); } catch (Exception e) { }

            String ef = request.getParameter("factor");
            String efv = request.getParameter("efv");
            String efo = request.getParameter("efo");

            AtlasSolrDAO.AtlasGeneResult atlasGene = atlasSolrDAO.getGeneByIdentifier(geneId);
            if(atlasGene.isFound()) {
                List<AtlasExperiment> exps = null;
                if(efo!=null){
                    exps = atlasSolrDAO.getRankedGeneExperimentsForEfo(atlasGene.getGene(), efo, fromRow, toRow);
                }else{
                    exps = atlasSolrDAO.getRankedGeneExperiments(atlasGene.getGene(), ef, efv,  fromRow, toRow);
                }
                request.setAttribute("exps",exps);
                request.setAttribute("atlasGene", atlasGene.getGene());
                request.getRequestDispatcher("/WEB-INF/jsp/genepage/experiment-list.jsp").include(request, response);
                return;
            }
        }

        //atlasSolrDAO.getExperimentsByQuery("");

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}

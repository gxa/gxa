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

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ListResultRow;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;

/**
 * @author pashky
 */
public class ExperimentPageRequestHandler implements HttpRequestHandler {

    private AtlasDao dao;
    private AtlasStructuredQueryService queryService;
    private File atlasNetCDFRepo;

    public void setDao(AtlasDao dao) {
        this.dao = dao;
    }

    public void setQueryService(AtlasStructuredQueryService queryService) {
        this.queryService = queryService;
    }

    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }


    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String expAcc = request.getParameter("eid");
        String geneIds = request.getParameter("gid");
        String ef = request.getParameter("ef");

        if (expAcc != null && !"".equals(expAcc)) {
            final AtlasExperiment exp = dao.getExperimentByAccession(expAcc);
            if (exp != null) {
                request.setAttribute("exp", exp);
                request.setAttribute("eid", exp.getId());

                List<ListResultRow> topGenes = queryService.findGenesForExperiment("", exp.getId(), 0, 10);
                request.setAttribute("geneList", topGenes);

                Collection<AtlasGene> genes = new ArrayList<AtlasGene>();
                if(geneIds != null) {
                    for(String geneQuery : StringUtils.split(geneIds, ",")) {
                        AtlasDao.AtlasGeneResult result = dao.getGeneByIdentifier(geneQuery);
                        if (result.isFound()) {
                            genes.add(result.getGene());
                        }
                    }
                } else {
                    for(ListResultRow lrr : topGenes.size() > 5 ? topGenes.subList(0, 5) : topGenes)
                        genes.add(lrr.getGene());
                }

                if(genes.isEmpty()) {
                    File[] netCDFs = atlasNetCDFRepo.listFiles(new FilenameFilter() {
                        public boolean accept(File file, String name) {
                            return name.matches("^" + exp.getId() + "_[0-9]+(_ratios)?\\.nc$");
                        }
                    });
                    if(netCDFs.length == 0) {
                        ErrorResponseHelper.errorNotFound(request, response, "NetCDF for experiment " + String.valueOf(expAcc) + " is not found");
                        return;
                    }
                    NetCDFProxy netcdf = new NetCDFProxy(netCDFs[0]);
                    for(int geneId : netcdf.getGenes()) {
                        AtlasDao.AtlasGeneResult result = dao.getGeneById(String.valueOf(geneId));
                        if (result.isFound()) {
                            genes.add(result.getGene());
                            if(genes.size() >= 5)
                                break;
                        }
                    }
                    String[] factors = netcdf.getFactors();
                    if(factors.length > 0)
                        ef = factors[0];
                    if(genes.isEmpty() || ef == null) {
                        ErrorResponseHelper.errorNotFound(request, response, "No genes to display for experiment " + String.valueOf(expAcc));
                        return;
                    }
                }

                request.setAttribute("genes", genes);
            } else {
                ErrorResponseHelper.errorNotFound(request, response, "There are no records for experiment " + String.valueOf(expAcc));
                return;
            }
        }

        request.setAttribute("ef", ef);
        request.getRequestDispatcher("/WEB-INF/jsp/experimentpage/experiment.jsp").forward(request, response);
    }

}

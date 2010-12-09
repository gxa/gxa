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

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.ListResultRow;
import ae3.service.structuredquery.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.ErrorResponseHelper;

/**
 * @author pashky
 */
@Deprecated
public class ExperimentPageRequestHandler implements HttpRequestHandler {

    private AtlasSolrDAO atlasSolrDAO;

    public void setDao(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Get search filters from request
        // Experiment accession
        String expAcc = StringUtils.trimToNull(request.getParameter("eid"));
        // Gene ids
        String geneIdsStr = StringUtils.trimToNull(request.getParameter("gid"));
        // Experimental factor
        String ef = StringUtils.trimToNull(request.getParameter("ef"));
        // Array design id
        String ad = StringUtils.trimToNull(request.getParameter("ad"));

        if (expAcc != null && !"".equals(expAcc)) {
            final AtlasExperiment exp = atlasSolrDAO.getExperimentByAccession(expAcc);
            if (exp != null) {
                request.setAttribute("exp", exp);
                request.setAttribute("eid", exp.getId());
                request.setAttribute("gid", geneIdsStr);
                request.setAttribute("ef", ef);
                request.setAttribute("arrayDesigns", exp.getPlatform().split(","));

                request.setAttribute("arrayDesign", exp.getArrayDesign(ad));
            } else {
                ErrorResponseHelper.errorNotFound(request, response, "No records exist for experiment " + String.valueOf(expAcc));
                return;
            }
        }

        request.getRequestDispatcher("/WEB-INF/jsp/experimentpage/experiment.jsp").forward(request, response);
    }
}

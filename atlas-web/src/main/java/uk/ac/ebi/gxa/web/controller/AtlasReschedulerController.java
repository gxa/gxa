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

package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.mvc.Controller;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.LoadStage;
import uk.ac.ebi.gxa.dao.LoadStatus;
import uk.ac.ebi.gxa.dao.LoadType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 14-Nov-2009
 */
@Deprecated
public class AtlasReschedulerController extends AbstractController {
    private AtlasDAO atlasDAO;
    private String successView;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public String getSuccessView() {
        return successView;
    }

    public void setSuccessView(String successView) {
        this.successView = successView;
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest,
                                                 HttpServletResponse httpServletResponse) throws Exception {
        // parse accession parameter
        String accession = ServletRequestUtils.getRequiredStringParameter(httpServletRequest, "accession");

        // parse type parameter
        String type = ServletRequestUtils.getRequiredStringParameter(httpServletRequest, "type");
        log.info("Request to schedule " + type + ": " + accession + " for re-indexing.");

        if (type.equals("experiment") || type.equals("gene")) {
            getAtlasDAO().writeLoadDetails(accession, LoadStage.SEARCHINDEX, LoadStatus.PENDING,
                                           (type.equals("experiment") ? LoadType.EXPERIMENT : LoadType.GENE));
            return new ModelAndView(getSuccessView());
        }
        else {
            String error = "the load type specified (" + type + ") is not permitted";

            // failure view
            Map<String, String> messageMap = new HashMap<String, String>();
            messageMap.put("message", error);
            return new ModelAndView("load_fail.jsp", messageMap);
        }
    }
}

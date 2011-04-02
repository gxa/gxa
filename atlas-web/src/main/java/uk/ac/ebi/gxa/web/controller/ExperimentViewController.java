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

import ae3.dao.ExperimentSolrDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.AtlasDAO;

/**
 * A code moved from ExperimentPageRequestHandler and ExperimentPage_DesignRequestHandler.
 *
 * @author Olga Melnichuk
 *         Date: Nov 29, 2010
 */
@Controller
public class ExperimentViewController extends ExperimentViewControllerBase {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewController.class);

    @Autowired
    public ExperimentViewController(ExperimentSolrDAO solrDAO, AtlasDAO atlasDAO) {
       super(solrDAO, atlasDAO);
    }

    @RequestMapping(value = "/experiment", method = RequestMethod.GET)
    public String getExperiment(
            @RequestParam("eid") String accession,
            @RequestParam(value = "ad", required = false) String ad,
            @RequestParam(value = "gid", required = false) String gid,
            @RequestParam(value = "ef", required = false) String ef,
            Model model) throws ResourceNotFoundException {

        ExperimentPage page = createExperimentPage(accession, ad);
        page.enhance(model);

        model.addAttribute("gid", gid);
        model.addAttribute("ef", ef);

        if (page.isExperimentInCuration()) {
            return "experimentpage/experiment-incuration";
        }

        return "experimentpage/experiment";
    }
}

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
import ae3.service.structuredquery.QueryExpression;
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

    /**
     * An experiment page handler
     *
     * @param accession an experiment accession to show experiment page for
     * @param ad an array design accession to show on the experiment page
     * @param gene a gene search string
     * @param ef an experiment factor to search genes for
     * @param efv an experiment factor value to search genes for
     * @param updown an updown filter to search genes for
     * @param offset an offset of the search results
     * @param limit a page size
     * @param model a model for the view to render
     * @return a view path
     * @throws ResourceNotFoundException if an experiment with the given accession is not found
     */
    @RequestMapping(value = "/experiment", method = RequestMethod.GET)
    public String getExperiment(
            @RequestParam("eid") String accession,
            @RequestParam(value = "ad", required = false) String ad,
            @RequestParam(value = "gid", required = false) String gene,
            @RequestParam(value = "ef", required = false) String ef,
            @RequestParam(value = "efv", required = false) String efv,
            @RequestParam(value = "updown", required = false) QueryExpression updown,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            Model model) throws ResourceNotFoundException {

        ExperimentPage page = createExperimentPage(accession, ad);
        page.enhance(model);

        model.addAttribute("gene", gene)
                .addAttribute("ef", ef)
                .addAttribute("efv", efv)
                .addAttribute("updown", updown)
                .addAttribute("offset", offset)
                .addAttribute("limit", limit);

        if (page.isExperimentInCuration()) {
            return "experimentpage/experiment-incuration";
        }

        return "experimentpage/experiment";
    }
}

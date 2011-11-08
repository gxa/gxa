/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.web.ui.plot.ThumbnailPlot;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.Arrays;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 */
@Controller
public class ThumbnailPlotViewController extends AtlasViewController {

    private final ExperimentDAO experimentDAO;
    private final GeneSolrDAO geneSolrDAO;
    private final AtlasDataDAO atlasDataDAO;

    @Autowired
    public ThumbnailPlotViewController(ExperimentDAO experimentDAO,
                                       GeneSolrDAO geneSolrDAO,
                                       AtlasDataDAO atlasDataDAO) {
        this.experimentDAO = experimentDAO;
        this.geneSolrDAO = geneSolrDAO;
        this.atlasDataDAO = atlasDataDAO;
    }

    @RequestMapping(value = "/deThumbnail", method = RequestMethod.GET)
    public String getExperimentPlot(
            @RequestParam("eacc") String expAccession,
            @RequestParam("deacc") String deacc,
            @RequestParam("ef") String ef,
            @RequestParam("efv") String efv,
            @RequestParam(value = "width", required = false, defaultValue = "90") Integer width,
            @RequestParam(value = "height", required = false, defaultValue = "45") Integer height,
            Model model
    ) throws RecordNotFoundException, AtlasDataException, StatisticsNotFoundException {

        final Experiment exp = experimentDAO.getByName(expAccession);
        ExperimentWithData ewd = null;
        try {
            ewd = atlasDataDAO.createExperimentWithData(exp);

            ExperimentPart expPart = new ExperimentPartCriteria()
                    .containsDeAccessions(Arrays.asList(deacc))
                    .retrieve(ewd);

            model.addAttribute("plot",
                    expPart == null ? null :
                            ThumbnailPlot.create(expPart, deacc, ef, efv)
                                    .scale(width, height)
                                    .asMap());
            return UNSUPPORTED_HTML_VIEW;
        } finally {
            closeQuietly(ewd);
        }
    }

    @RequestMapping(value = "/geneThumbnail", method = RequestMethod.GET)
    public String getExperimentPlot(
            @RequestParam("eacc") String expAccession,
            @RequestParam("gid") Long geneId,
            @RequestParam("ef") String ef,
            @RequestParam("efv") String efv,
            @RequestParam(value = "width", required = false, defaultValue = "90") Integer width,
            @RequestParam(value = "height", required = false, defaultValue = "45") Integer height,
            Model model
    ) throws RecordNotFoundException, AtlasDataException, StatisticsNotFoundException {
        GeneSolrDAO.AtlasGeneResult geneResult = geneSolrDAO.getGeneById(geneId);
        AtlasGene gene = (geneResult.isFound()) ? geneResult.getGene() : null;
        if (gene == null) {
            throw new RecordNotFoundException("No gene found for id = " + geneId);
        }

        final Experiment exp = experimentDAO.getByName(expAccession);
        ExperimentWithData ewd = null;
        try {
            ewd = atlasDataDAO.createExperimentWithData(exp);

            List<Long> geneIds = Arrays.asList(geneId);
            ExperimentPart expPart = new ExperimentPartCriteria()
                    .containsGenes(geneIds)
                    .containsEfEfv(ef, efv)
                    .retrieve(ewd);

            model.addAttribute("plot",
                    expPart == null ? null :
                            ThumbnailPlot.create(expPart, geneId, ef, efv)
                                    .scale(width, height)
                                    .asMap());
            return UNSUPPORTED_HTML_VIEW;
        } finally {
            closeQuietly(ewd);
        }
    }
}

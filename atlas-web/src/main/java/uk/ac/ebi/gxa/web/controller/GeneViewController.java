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

import ae3.anatomogram.Anatomogram;
import ae3.anatomogram.Annotator;
import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.AtlasGeneDescription;
import com.google.common.io.Closeables;
import org.apache.batik.transcoder.TranscoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * The code is originally from GenePageRequestHandler, AnatomogramRequestHandler and ExperimentsListRequestHandler.
 *
 * @author Olga Melnichuk
 *         Date: Dec 10, 2010
 */
@Controller
public class GeneViewController extends AtlasViewController {

    private AtlasSolrDAO atlasSolrDAO;
    private AtlasProperties atlasProperties;
    private Annotator annotator;

    @Autowired
    public GeneViewController(AtlasSolrDAO atlasSolrDAO, AtlasProperties atlasProperties, Annotator annotator) {
        this.atlasSolrDAO = atlasSolrDAO;
        this.atlasProperties = atlasProperties;
        this.annotator = annotator;
    }

    @RequestMapping(value = "/gene", method = RequestMethod.GET)
    public String getGene(
            @RequestParam("gid") String geneId,
            @RequestParam(value = "ef", required = false) String ef,
            Model model
    ) throws ResourceNotFoundException, IOException, TranscoderException {

        AtlasSolrDAO.AtlasGeneResult result = atlasSolrDAO.getGeneByAnyIdentifier(geneId, atlasProperties.getGeneAutocompleteIdFields());
        if (result.isMulti()) {
            model.addAttribute("gprop_0", "")
                    .addAttribute("gval_0", geneId)
                    .addAttribute("fexp_0", "UP_DOWN")
                    .addAttribute("fact_0", "")
                    .addAttribute("specie_0", "")
                    .addAttribute("fval_0", "(all+conditions)")
                    .addAttribute("view", "hm");
            return "redirect:qrs";
        }

        if (!result.isFound()) {
            throw new ResourceNotFoundException("No results were found");
        }

        AtlasGene gene = result.getGene();
        Anatomogram an = annotator.getAnatomogram(getAnatomogramType(null), gene);

        model.addAttribute("anatomogramMap", an.getAreaMap())
                .addAttribute("orthologs", atlasSolrDAO.getOrthoGenes(gene))
                .addAttribute("heatMapRows", gene.getHeatMap(atlasProperties.getGeneHeatmapIgnoredEfs()).getValueSortedList())
                .addAttribute("differentiallyExpressedFactors", gene.getDifferentiallyExpressedFactors(atlasProperties.getGeneHeatmapIgnoredEfs(), atlasSolrDAO, ef))
                .addAttribute("atlasGene", gene)
                .addAttribute("ef", ef)
                .addAttribute("atlasGeneDescription", new AtlasGeneDescription(atlasProperties, gene).toString())
                .addAttribute("hasAnatomogram", !an.isEmpty())
                .addAttribute("noAtlasExps", gene.getNumberOfExperiments(ef));

        return "genepage/gene";
    }

    @RequestMapping(value = "/geneIndex", method = RequestMethod.GET)
    public String getGeneIndex() {
        return "genepage/gene-index";
    }

    @RequestMapping(value = "/anatomogram", method = RequestMethod.GET)
    @ResponseBody
    public byte[] getAnatomogramImage(
            @RequestParam("gid") String geneId,
            @RequestParam(value = "type", required = false) String aType
    ) throws IOException, TranscoderException {

        Annotator.AnatomogramType anatomogramType = getAnatomogramType(aType);
        Anatomogram an = annotator.getEmptyAnatomogram();

        AtlasSolrDAO.AtlasGeneResult geneResult = atlasSolrDAO.getGeneByIdentifier(geneId);
        if (geneResult.isFound()) {
            an = annotator.getAnatomogram(anatomogramType, geneResult.getGene());
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            an.writePngToStream(bytes);
            return bytes.toByteArray();
        } finally {
            Closeables.closeQuietly(bytes);
        }
    }

    @RequestMapping(value = "/geneExpList")
    public String getExperimentsList(
            @RequestParam("gid") String geneId,
            @RequestParam(value = "from", required = false) Integer from,
            @RequestParam(value = "to", required = false) Integer to,
            @RequestParam(value = "factor", required = false) String ef,
            @RequestParam(value = "efv", required = false) String efv,
            @RequestParam(value = "efo", required = false) String efo,
            Model model
    ) throws ResourceNotFoundException {

        int fromRow = from == null ? -1 : from;
        int toRow = to == null ? -1 : to;

        AtlasSolrDAO.AtlasGeneResult result = atlasSolrDAO.getGeneByIdentifier(geneId);
        if (!result.isFound()) {
            throw new ResourceNotFoundException("Gene not found id=" + geneId);
        }

        AtlasGene gene = result.getGene();
        List<AtlasExperiment> exps = efo != null ?
                atlasSolrDAO.getRankedGeneExperimentsForEfo(gene, efo, fromRow, toRow) :
                atlasSolrDAO.getRankedGeneExperiments(gene, ef, efv, fromRow, toRow);

        model.addAttribute("exps", exps)
                .addAttribute("atlasGene", gene);

        return "genepage/experiment-list";
    }

    private static Annotator.AnatomogramType getAnatomogramType(String aType) {
        return aType == null ? Annotator.AnatomogramType.Das : Annotator.AnatomogramType.valueOf(capitalize(aType));
    }

    private static String capitalize(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + (str.length() > 1 ? str.substring(1).toLowerCase() : "");
    }
}


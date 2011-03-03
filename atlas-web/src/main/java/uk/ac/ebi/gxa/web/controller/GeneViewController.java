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

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.AtlasGeneDescription;
import ae3.service.AtlasStatisticsQueryService;
import com.google.common.io.Closeables;
import org.apache.batik.transcoder.TranscoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.gxa.anatomogram.Anatomogram;
import uk.ac.ebi.gxa.anatomogram.AnatomogramFactory;
import uk.ac.ebi.gxa.dao.GeneDAO;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.StringUtil;
import uk.ac.ebi.microarray.atlas.model.Gene;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
    private AnatomogramFactory anatomogramFactory;
    private AtlasStatisticsQueryService atlasStatisticsQueryService;
    private GeneDAO geneDAO;
    private Efo efo;

    final private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    public GeneViewController(AtlasSolrDAO atlasSolrDAO, AtlasProperties atlasProperties,
                              AnatomogramFactory anatomogramFactory,
                              AtlasStatisticsQueryService atlasStatisticsQueryService,
                              GeneDAO geneDAO,
                              Efo efo) {
        this.atlasSolrDAO = atlasSolrDAO;
        this.atlasProperties = atlasProperties;
        this.anatomogramFactory = anatomogramFactory;
        this.atlasStatisticsQueryService = atlasStatisticsQueryService;
        this.geneDAO = geneDAO;
        this.efo = efo;
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
        Anatomogram an = anatomogramFactory.getAnatomogram(gene);

        model.addAttribute("orthologs", atlasSolrDAO.getOrthoGenes(gene))
                .addAttribute("differentiallyExpressedFactors", gene.getDifferentiallyExpressedFactors(atlasProperties.getGeneHeatmapIgnoredEfs(), ef, atlasStatisticsQueryService))
                .addAttribute("atlasGene", gene)
                .addAttribute("ef", ef)
                .addAttribute("atlasGeneDescription", new AtlasGeneDescription(atlasProperties, gene, atlasStatisticsQueryService).toString())
                .addAttribute("hasAnatomogram", !an.isEmpty())
                .addAttribute("anatomogramMap", an.getAreaMap())
                .addAttribute("noAtlasExps", gene.getNumberOfExperiments(ef, atlasStatisticsQueryService));

        return "genepage/gene";
    }

    /**
     * Retrives a list of genes by prefix and offset.
     *
     * @param offset an offset within a list of genes with the given prefix
     * @param prefix a prefix to find genes with
     * @param model a model object returned to the view
     * @return the view name
     */
    @RequestMapping(value = "/geneIndex", method = RequestMethod.GET)
    public String getGeneIndex(
            @RequestParam(value = "offset", required = false) Integer offset,
            @RequestParam(value = "prefix", required = false) String prefix,
            Model model
    ) {
        prefix = prefix == null ? "a" : prefix;
        offset = offset == null ? 1 : offset;

        int pageSize = 100;

        Collection<Gene> genes = geneDAO.getGenes(prefix, offset, pageSize);

        model.addAttribute("genes", genes);
        model.addAttribute("nextQuery", (genes.size() < pageSize) ? "" :
                "?prefix=" + prefix + "&offset=" + (offset + pageSize));

        return "genepage/gene-index";
    }

    @RequestMapping(value = "/anatomogram", method = RequestMethod.GET)
    @ResponseBody
    public byte[] getAnatomogramImage(
            @RequestParam("gid") String geneId,
            @RequestParam(value = "type", required = false) String aType
    ) throws IOException, TranscoderException {

        /**
         * Note: DAS anatomograms are used by external EBI Services only
         * E.g. http://www.ebi.ac.uk/s4/eyeresult/?node=expression&term=ENSG00000012048 */
        AnatomogramFactory.AnatomogramType anatomogramType = aType == null ?
                AnatomogramFactory.AnatomogramType.Das : AnatomogramFactory.AnatomogramType.valueOf(StringUtil.upcaseFirst(aType));

        Anatomogram an = anatomogramFactory.getEmptyAnatomogram();

        AtlasSolrDAO.AtlasGeneResult geneResult = atlasSolrDAO.getGeneByIdentifier(geneId);
        if (geneResult.isFound()) {
            an = anatomogramFactory.getAnatomogram(anatomogramType, geneResult.getGene());
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
            @RequestParam(value = "efo", required = false) String efoId,
            Model model
    ) throws ResourceNotFoundException {

        int fromRow = from == null ? -1 : from;
        int toRow = to == null ? -1 : to;

        AtlasSolrDAO.AtlasGeneResult result = atlasSolrDAO.getGeneByIdentifier(geneId);
        if (!result.isFound()) {
            throw new ResourceNotFoundException("Gene not found id=" + geneId);
        }

        AtlasGene gene = result.getGene();
        Attribute attr =
                efoId != null ?
                        new EfoAttribute(efoId, StatisticsType.UP_DOWN) :
                        new EfvAttribute(ef, efv, StatisticsType.UP_DOWN);

        List<AtlasExperiment> exps =  getRankedGeneExperiments(gene, attr, fromRow, toRow) ;

        model.addAttribute("exps", exps)
                .addAttribute("atlasGene", gene)
                .addAttribute("target", efoId == null ?
                        (efv == null ? "" : efv) :
                        efoId + ": " + efo.getTermById(efoId).getTerm());

        return "genepage/experiment-list";
    }


    /**
     * @param gene     gene of interest
     * @param attribute
     * @param fromRow
     * @param toRow
     * @return List of AtlasExperiments, sorted by pVal/tStat rank - best first w.r.t to gene and ef-efv
     */
    private List<AtlasExperiment> getRankedGeneExperiments(AtlasGene gene, Attribute attribute, int fromRow, int toRow) {
        long start = System.currentTimeMillis();
        List<AtlasExperiment> sortedAtlasExps = new ArrayList<AtlasExperiment>();

        List<Experiment> sortedExps = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(gene.getGeneId(), attribute, fromRow, toRow);
        log.debug("Retrieved " + sortedExps.size() + " experiments from bit index in: " + (System.currentTimeMillis() - start) + " ms");
        for (Experiment exp : sortedExps) {
            AtlasExperiment atlasExperiment = atlasSolrDAO.getExperimentById(exp.getExperimentId());
            if (atlasExperiment != null) {
                EfvAttribute efAttr = exp.getHighestRankAttribute();
                if (efAttr != null && efAttr.getEf() != null) {
                    atlasExperiment.setHighestRankEF(efAttr.getEf());
                } else {
                    log.error("Failed to find highest rank attribute in: " + exp);
                }
                sortedAtlasExps.add(atlasExperiment);

            } else {
                log.error("Failed to find experiment: " + exp + " in Solr experiment index");
            }
        }
        return sortedAtlasExps;
    }
}


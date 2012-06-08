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

import ae3.model.AtlasGene;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.PropertyDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.data.StatisticsNotFoundException;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.service.experiment.ExperimentAnalytics;
import uk.ac.ebi.gxa.service.experiment.ExperimentDataService;
import uk.ac.ebi.gxa.web.ui.NameValuePair;
import uk.ac.ebi.gxa.web.ui.plot.AssayProperties;
import uk.ac.ebi.gxa.web.ui.plot.ExperimentPlot;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Joiner.on;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * @author Olga Melnichuk
 */
@Controller
public class ExperimentViewController extends ExperimentViewControllerBase {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewController.class);

    private final ExperimentDataService expDataService;

    private final PropertyDAO propertyDAO;

    private final AtlasProperties atlasProperties;

    private final Function<String, String> curatedStringConverter = new Function<String, String>() {
        @Override
        public String apply(@Nullable String input) {
            try {
                return propertyDAO.getByName(input).getDisplayName();
            } catch (RecordNotFoundException e) {
                throw createUnexpected("Cannot find property " + input, e);
            }
        }
    };

    @Autowired
    public ExperimentViewController(ExperimentDataService expDataService,
                                    PropertyDAO propertyDAO,
                                    AtlasProperties atlasProperties) {
        super(expDataService);
        this.propertyDAO = propertyDAO;
        this.atlasProperties = atlasProperties;
        this.expDataService = expDataService;
    }

    /**
     * Handles experiment page requests with known experiment accession.<br/>
     * Gene identifier and experimental factor parameters are optional.<br/>
     * <ul>Request uri examples:
     * <li>/experiment?eid=E-MTAB-62/</li>
     * <li>/experiment?eid=E-MTAB-62&gid=KRT7</li>
     * <li>/experiment?eid=E-MTAB-62&ef=organism_part</li>
     * <li>/experiment?eid=E-MTAB-62&gid=KRT7&ef=organism_part</li>
     * </ul>
     *
     * @param accession an experiment accession to show experiment details for
     * @param gid       a gene identifier to search for by default
     * @param ef        an experimental factor to search for by default
     * @param model     a model to render the view
     * @return path of the view to render
     * @throws RecordNotFoundException if an experiment with the given accession not found
     */
    @RequestMapping(value = "/experiment", method = RequestMethod.GET)
    public String getExperimentByAccessionParam(
            @RequestParam("eid") String accession,
            @RequestParam(value = "gid", required = false) String gid,
            @RequestParam(value = "ef", required = false) String ef,
            Model model) throws RecordNotFoundException {
        return getExperiment(model, accession, gid, ef);
    }

    /**
     * Handles experiment page requests with known experiment accession.<br/>
     * Gene identifier and experimental factor parameters are optional.<br/>
     * <ul>Request uri examples:
     * <li>/experiment/E-MTAB-62/</li>
     * <li>/experiment/E-MTAB-62?gid=KRT7</li>
     * <li>/experiment/E-MTAB-62?ef=organism_part</li>
     * <li>/experiment/E-MTAB-62?gid=KRT7&ef=organism_part</li>
     * </ul>
     *
     * @param accession an experiment accession to show experiment details for
     * @param gid       a gene identifier to search for by default
     * @param ef        an experimental factor to search for by default
     * @param model     a model to render the view
     * @return path of the view to render
     * @throws RecordNotFoundException if an experiment with the given accession not found
     */
    @RequestMapping(value = "/experiment/{eid}", method = RequestMethod.GET)
    public String getExperimentByAccession(
            @PathVariable("eid") final String accession,
            @RequestParam(value = "gid", required = false) String gid,
            @RequestParam(value = "ef", required = false) String ef,
            Model model) throws RecordNotFoundException {
        return getExperiment(model, accession, gid, ef);
    }

    /**
     * Handles experiment page requests with known experiment accession and gene identifier. <br/>
     * Only experimental factor parameter is optional. <br/>
     * <ul>Request uri examples:
     * <li>/experiment/E-MTAB-62/KRT7</li>
     * <li>/experiment/E-MTAB-62/KRT7&ef=organism_part</li>
     * </ul>
     *
     * @param accession an experiment accession to show experiment details for
     * @param gid       a gene identifier to search for by default
     * @param ef        an experimental factor to search for by default
     * @param model     a model to render the view
     * @return path of the view to render
     * @throws RecordNotFoundException if an experiment with the given accession not found
     */
    @RequestMapping(value = "/experiment/{eid}/{gid}", method = RequestMethod.GET)
    public String getExperimentByAccessionAndGene(
            @PathVariable("eid") final String accession,
            @PathVariable("gid") final String gid,
            @RequestParam(value = "ef", required = false) String ef,
            Model model) throws RecordNotFoundException {
        return getExperiment(model, accession, gid, ef);
    }

    /**
     * Handles experiment page requests with known experiment accession, gene identifier and experimental factor.<br/>
     * <ul>Request uri examples:
     * <li>/experiment/E-MTAB-62/KRT7/organism_part</li>
     * </ul>
     *
     * @param accession an experiment accession to show experiment details for
     * @param gid       a gene identifier to search for by default
     * @param ef        an experimental factor to search for by default
     * @param model     a model to render the view
     * @return path of the view to render
     * @throws RecordNotFoundException if an experiment with the given accession not found
     */
    @RequestMapping(value = "/experiment/{eid}/{gid}/{ef}", method = RequestMethod.GET)
    public String getExperimentByAccessionAndGeneAndEf(
            @PathVariable("eid") final String accession,
            @PathVariable("gid") final String gid,
            @PathVariable("ef") final String ef,
            Model model) throws RecordNotFoundException {
        return getExperiment(model, accession, gid, ef);
    }

    /**
     * Handles experiment plot requests.
     * (JSON view only supported)
     *
     * @param accession               an experiment accession to fetch the data for
     * @param adAcc                   an array design accession to fetch the data for
     * @param des                     an array of design element indices to fetch data for
     * @param assayPropertiesRequired a boolean value; just a flag to load assay properties only once
     * @param model                   a model to render the view
     * @return path of the view to render
     * @throws RecordNotFoundException if an experiment or array design is not found
     * @throws AtlasDataException        if any data reading error happened (including index out of range)
     */
    @RequestMapping(value = "/experimentPlot", method = RequestMethod.GET)
    public String getExperimentPlot(
            @RequestParam("eid") String accession,
            @RequestParam("ad") String adAcc,
            @RequestParam("de") int[] des,
            @RequestParam(value = "assayPropertiesRequired", required = false, defaultValue = "false") Boolean assayPropertiesRequired,
            Model model
    ) throws RecordNotFoundException, AtlasDataException {
        final ExperimentPage page = createExperimentPage(accession);
        final ArrayDesign ad = page.getExperiment().getArrayDesign(adAcc);
        if (ad == null) {
            throw new RecordNotFoundException("Unknown array design accession: " + adAcc + " (in " + accession + " experiment)");
        }

        final Experiment experiment = page.getExperiment();
        ExperimentWithData ewd = null;
        try {
            ewd = expDataService.getExperimentWithData(experiment);
            model.addAttribute("plot", ExperimentPlot.create(des, ewd, ad, curatedStringConverter));
            if (assayPropertiesRequired) {
                model.addAttribute("assayProperties", AssayProperties.create(ewd, ad, curatedStringConverter));
            }
            return JSON_ONLY_VIEW;
        } finally {
            closeQuietly(ewd);
        }
    }

    /**
     * This method HTTP GET's assetFileName's content for a given experiment provided that
     * 1. assetFileName is listed against that experiment in DB
     * 2. assetFileName has a file extension corresponding to a valid experiment asset mime type (c.f. ResourceType)
     *
     * @param accession     experiment accession
     * @param assetFileName asset file name
     * @param response      HttpServletResponse
     * @throws IOException
     * @throws ResourceNotFoundException
     * @throws uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException
     */
    @RequestMapping(value = "/assets", method = RequestMethod.GET)
    public void getExperimentAsset(
            @RequestParam("eid") String accession,
            @RequestParam("asset") String assetFileName,
            HttpServletResponse response) throws IOException, ResourceNotFoundException, RecordNotFoundException {
        send(response, expDataService.getAssetFile(accession, assetFileName));
    }

    /**
     * Returns experiment table data for given search parameters.
     * (JSON view only supported)
     *
     * @param accession an experiment accession to find out the required data
     * @param adAcc     an array design accession to find out the required data
     * @param geneConditions a gene param to search with
     * @param ef        an experiment factor param to search with
     * @param efv       an experiment factor value param to search with
     * @param updown    an up/down condition to search with
     * @param offset    an offset of results to take
     * @param limit     a size of result set to take
     * @param model     a model for the view to render
     * @return the view path
     * @throws AtlasDataException or StatisticsNotFoundException if data could not be read
     */
    @RequestMapping(value = "/experimentTable", method = RequestMethod.GET)
    public String getExperimentTable(
            @RequestParam("eacc") String accession,
            @RequestParam(value = "ad", required = false) String adAcc,
            @RequestParam(value = "geneConditions[]", required = false) String[] geneConditions,
            @RequestParam(value = "gid", required = false) String gid,
            @RequestParam(value = "ef", required = false) String ef,
            @RequestParam(value = "efv", required = false) String efv,
            @RequestParam(value = "updown", required = false, defaultValue = "CONDITION_ANY") UpDownCondition updown,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit,
            Model model
    ) throws ResourceNotFoundException, RecordNotFoundException, AtlasDataException, StatisticsNotFoundException {
        if (limit > 200) {
            log.warn("Page size is: {} {}", new String[]{String.valueOf(limit), accession});
        }

        //ToDo: maybe there is a way to fix it on the js side.
        if (geneConditions == null && gid != null) {
            final List<String> conditions = Lists.newArrayList(Splitter.on(',').split(gid));
            geneConditions = new String[conditions.size()];
            geneConditions = conditions.toArray(geneConditions);
        }
        ExperimentAnalytics analytics = expDataService.getExperimentAnalytics(accession, adAcc, geneConditions, ef, efv, updown, offset, limit);

        model.addAttribute("analytics", analytics);
        model.addAttribute("pageSize", limit);
        model.addAttribute("geneToolTips", getGeneTooltips(analytics.getGenes()));
        return "experimentTable";
    }

    private Map<String, GeneToolTip> getGeneTooltips(Collection<AtlasGene> genes) {
        Map<String, GeneToolTip> tips = new HashMap<String, GeneToolTip>(genes.size());
        for (AtlasGene gene : genes) {
            tips.put("" + gene.getGeneId(), new GeneToolTip(gene));
        }
        return tips;
    }

    /**
     * An experiment page handler utility. If the experiment with the given id/accession exists it fills the model with the
     * appropriate values and returns the corresponding view. E.g. /experiment/E-MTAB-62/ENSG00000136487/organism_part
     * note that gid and ef are optional.
     *
     * @param model
     * @param accession
     * @param gid
     * @param ef
     * @return
     * @throws RecordNotFoundException
     */
    private String getExperiment(Model model, String accession, @Nullable final String gid, @Nullable final String ef) throws RecordNotFoundException {
        JsMapModel jsMapModel = JsMapModel.wrap(model);

        ExperimentPage page = createExperimentPage(accession);
        page.enhance(jsMapModel);

        jsMapModel
                .addJsAttribute("eid", page.getExperiment().getAccession())
                .addJsAttribute("gid", gid)
                .addJsAttribute("ef", ef)
                .addJsAttribute("arrayDesigns", Joiner.on(" ").join(page.getArrayDesigns()));

        if (page.isExperimentInCuration()) {
            return "experimentpage/experiment-incuration";
        }

        return "experimentpage/experiment";
    }

    private class GeneToolTip {
        private AtlasGene gene;

        public GeneToolTip(final AtlasGene gene) {
            this.gene = gene;
        }

        @JsonProperty("name")
        public String getName() {
            return gene.getGeneName();
        }

        @JsonProperty("identifiers")
        public String getIdentifiers() {
            final Map<String, Collection<String>> properties = gene.getGeneProperties();
            Collection<String> geneIdentifiers = Collections2.transform(atlasProperties.getGeneAutocompleteNameFields(),
                    new Function<String, String>() {
                        public String apply(@Nonnull String geneProperty) {
                            return on(",").join(properties.get(geneProperty));
                        }
                    });
            return on(",").join(geneIdentifiers);
        }

        @JsonProperty("properties")
        public Collection<NameValuePair<String>> getProperties() {
            final Map<String, String> curatedProperties = atlasProperties.getCuratedGeneProperties();
            Collection<NameValuePair<String>> geneProperties = Collections2.transform(atlasProperties.getGeneTooltipFields(),
                    new Function<String, NameValuePair<String>>() {
                        @Override
                        public NameValuePair<String> apply(@Nullable String input) {
                            return new NameValuePair<String>(
                                    curatedProperties.get(input),
                                    gene.getPropertyValue(input));
                        }
                    });
            return geneProperties;
        }
    }


}

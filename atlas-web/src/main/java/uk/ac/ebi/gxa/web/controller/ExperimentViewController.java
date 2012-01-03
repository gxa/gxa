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

import ae3.dao.ExperimentSolrDAO;
import ae3.dao.GeneSolrDAO;
import ae3.model.AtlasGene;
import ae3.service.experiment.AtlasExperimentAnalyticsViewService;
import ae3.service.experiment.BestDesignElementsResult;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
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
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.PropertyDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.gxa.web.ui.NameValuePair;
import uk.ac.ebi.gxa.web.ui.plot.AssayProperties;
import uk.ac.ebi.gxa.web.ui.plot.ExperimentPlot;
import uk.ac.ebi.microarray.atlas.model.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Predicates.*;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;
import static uk.ac.ebi.gxa.utils.NumberFormatUtil.formatPValue;
import static uk.ac.ebi.gxa.utils.NumberFormatUtil.formatTValue;
import static uk.ac.ebi.gxa.utils.Pair.create;

/**
 * @author Olga Melnichuk
 */
@Controller
public class ExperimentViewController extends ExperimentViewControllerBase {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewController.class);

    private final AtlasDataDAO atlasDataDAO;

    private final GeneSolrDAO geneSolrDAO;

    private final PropertyDAO propertyDAO;

    private final AtlasProperties atlasProperties;

    private AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService;

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
    public ExperimentViewController(ExperimentSolrDAO solrDAO,
                                    GeneSolrDAO geneSolrDAO,
                                    AtlasDAO atlasDAO,
                                    AtlasDataDAO atlasDataDAO,
                                    PropertyDAO propertyDAO,
                                    AtlasExperimentAnalyticsViewService atlasExperimentAnalyticsViewService,
                                    AtlasProperties atlasProperties) {
        super(solrDAO, atlasDAO);
        this.atlasDataDAO = atlasDataDAO;
        this.propertyDAO = propertyDAO;
        this.atlasProperties = atlasProperties;
        this.geneSolrDAO = geneSolrDAO;
        this.atlasExperimentAnalyticsViewService = atlasExperimentAnalyticsViewService;
    }

    protected List<Long> findGeneIds(Collection<String> geneQuery) {
        return geneSolrDAO.findGeneIds(geneQuery);
    }

    /**
     * An experiment page handler. If the experiment with the given id/accession exists it fills the model with the
     * appropriate values and returns the corresponding view. E.g. /experiment/E-MTAB-62/
     * Parameter gid (geneid) is optional - this is to support queries such as: /experiment/E-GEOD-5099?ad=A-AFFY-34&gid=GBP5
     *
     * @param accession an experiment accession to show experiment page for
     * @param model     a model for the view to render
     * @return path of the view
     * @throws ResourceNotFoundException if an experiment with the given accession is not found
     */
    @RequestMapping(value = "/experiment/{eid}", method = RequestMethod.GET)
    public String getExperimentWithOptionalGene(
            @PathVariable("eid") final String accession,
            @RequestParam(value = "gid", required = false) String gid,
            Model model) throws ResourceNotFoundException {

        return getExperiment(model, accession, gid, null);
    }

    /**
     * An experiment page handler. If the experiment with the given id/accession exists it fills the model with the
     * appropriate values and returns the corresponding view. E.g. /experiment/E-MTAB-62/ENSG00000136487).
     *
     * @param accession an experiment accession to show experiment page for
     * @param gid       a gene identifier to fill in initial search fields
     * @param model     a model for the view to render
     * @return path of the view
     * @throws ResourceNotFoundException if an experiment with the given accession is not found
     */
    @RequestMapping(value = "/experiment/{eid}/{gid}", method = RequestMethod.GET)
    public String getExperiment(
            @PathVariable("eid") final String accession,
            @PathVariable("gid") final String gid,
            Model model) throws ResourceNotFoundException {

        return getExperiment(model, accession, gid, null);
    }

    /**
     * An experiment page handler. If the experiment with the given id/accession exists it fills the model with the
     * appropriate values and returns the corresponding view. E.g. /experiment/E-MTAB-62/ENSG00000136487/organism_part).
     *
     * @param accession an experiment accession to show experiment page for
     * @param gid       a gene identifier to fill in initial search fields
     * @param ef        an experiment factor name to fill in initial search fields
     * @param model     a model for the view to render
     * @return path of the view
     * @throws ResourceNotFoundException if an experiment with the given accession is not found
     */
    @RequestMapping(value = "/experiment/{eid}/{gid}/{ef}", method = RequestMethod.GET)
    public String getExperiment(
            @PathVariable("eid") final String accession,
            @PathVariable("gid") final String gid,
            @PathVariable("ef") final String ef,
            Model model) throws ResourceNotFoundException {

        return getExperiment(model, accession, gid, ef);
    }

    /**
     * An experiment page handler. If the experiment with the given id/accession exists it fills the model with the
     * appropriate values and returns the corresponding view. Parameters like gid (geneId) and ef (experiment factor)
     * are optional; they can be specified manually e.g.  /experiment?eid=E-MTAB-62&gid=ENSG00000136487&ef=organism_part
     *
     * @param accession an experiment accession to show experiment page for
     * @param gid       a gene identifier to fill in initial search fields
     * @param ef        an experiment factor name to fill in initial search fields
     * @param model     a model for the view to render
     * @return path of the view
     * @throws ResourceNotFoundException if an experiment with the given accession is not found
     */
    @RequestMapping(value = "/experiment", method = RequestMethod.GET)
    public String getExperimentWithOptionalParams(
            @RequestParam("eid") String accession,
            @RequestParam(value = "gid", required = false) String gid,
            @RequestParam(value = "ef", required = false) String ef,
            Model model) throws ResourceNotFoundException {
        return getExperiment(model, accession, gid, ef);
    }

    /**
     * Returns experiment plots for given set of design elements.
     * (JSON view only supported)
     *
     * @param accession               an experiment accession to find out the required data
     * @param adAcc                   an array design accession to find out the required data
     * @param des                     an array of design element indexes to get plot data for
     * @param assayPropertiesRequired a boolean value to specify if assay properties ard needed
     * @param model                   a model for the view to render
     * @return the view path
     * @throws ResourceNotFoundException if an experiment or array design is not found
     * @throws AtlasDataException        if any data reading error happened (including index out of range)
     */
    @RequestMapping(value = "/experimentPlot", method = RequestMethod.GET)
    public String getExperimentPlot(
            @RequestParam("eid") String accession,
            @RequestParam("ad") String adAcc,
            @RequestParam("de") int[] des,
            @RequestParam(value = "assayPropertiesRequired", required = false, defaultValue = "false") Boolean assayPropertiesRequired,
            Model model
    ) throws ResourceNotFoundException, AtlasDataException, RecordNotFoundException {

        final ExperimentPage page = createExperimentPage(accession);
        final ArrayDesign ad = page.getExperiment().getArrayDesign(adAcc);
        if (ad == null) {
            throw new ResourceNotFoundException("Improper array design accession: " + adAcc + " (in " + accession + " experiment)");
        }

        final Experiment experiment = atlasDAO.getExperimentByAccession(accession);
        ExperimentWithData ewd = null;
        try {
            ewd = atlasDataDAO.createExperimentWithData(experiment);
            model.addAttribute("plot", ExperimentPlot.create(des, ewd, ad, curatedStringConverter));
            if (assayPropertiesRequired) {
                model.addAttribute("assayProperties", AssayProperties.create(ewd, ad, curatedStringConverter));
            }
            return UNSUPPORTED_HTML_VIEW;
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
     */
    @RequestMapping(value = "/assets", method = RequestMethod.GET)
    public void getExperimentAsset(
            @RequestParam("eid") String accession,
            @RequestParam("asset") String assetFileName,
            HttpServletResponse response) throws IOException, ResourceNotFoundException, RecordNotFoundException {

        if (isNullOrEmpty(accession) || isNullOrEmpty(assetFileName))
            throw new ResourceNotFoundException("Incomplete request");

        Experiment experiment = atlasDAO.getExperimentByAccession(accession);

        Asset asset = experiment.getAsset(assetFileName);
        if (asset == null)
            throw assetNotFound(accession, assetFileName);

        final File assetFile = new File(new File(atlasDataDAO.getDataDirectory(experiment), "assets"), asset.getFileName());
        if (!assetFile.exists())
            throw assetNotFound(accession, assetFileName);

        ResourceType type = ResourceType.getByFileName(assetFileName);
        if (type == null)
            throw assetNotFound(accession, assetFileName);

        send(response, assetFile, type);
    }

    private ResourceNotFoundException assetNotFound(String accession, String assetFileName) {
        return new ResourceNotFoundException("Asset: " + assetFileName + " not found for experiment: " + accession);
    }

    /**
     * Returns experiment table data for given search parameters.
     * (JSON view only supported)
     *
     * @param accession an experiment accession to find out the required data
     * @param adAcc     an array design accession to find out the required data
     * @param gid       a gene param to search with
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
            @RequestParam(value = "gid", required = false) String gid,
            @RequestParam(value = "ef", required = false) String ef,
            @RequestParam(value = "efv", required = false) String efv,
            @RequestParam(value = "updown", required = false, defaultValue = "CONDITION_ANY") UpDownCondition updown,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit,
            Model model
    ) throws ResourceNotFoundException, RecordNotFoundException, AtlasDataException, StatisticsNotFoundException {
        ExperimentWithData ewd = null;
        try {
            final Experiment experiment = atlasDAO.getExperimentByAccession(accession);
            ewd = atlasDataDAO.createExperimentWithData(experiment);

            final Predicate<Long> geneIdPredicate = genePredicate(gid);

            ExperimentPartCriteria criteria = ExperimentPartCriteria.experimentPart();
            if (!isNullOrEmpty(adAcc)) {
                criteria.hasArrayDesignAccession(adAcc);
            } else {
                criteria.containsAtLeastOneGene(geneIdPredicate);
            }


            BestDesignElementsResult res = atlasExperimentAnalyticsViewService.findBestGenesForExperiment(
                    criteria.retrieveFrom(ewd),
                    geneIdPredicate, updown,
                    createFactorCriteria(ef, efv),
                    offset,
                    limit
            );

            model.addAttribute("arrayDesign", res.getArrayDesignAccession());
            model.addAttribute("totalSize", res.getTotalSize());
            model.addAttribute("items", Iterables.transform(res,
                    new Function<BestDesignElementsResult.Item, ExperimentTableRow>() {
                        public ExperimentTableRow apply(@Nullable BestDesignElementsResult.Item item) {
                            return item == null ? null : new ExperimentTableRow(item);
                        }
                    })
            );
            model.addAttribute("geneToolTips", getGeneTooltips(res.getGenes()));
            return UNSUPPORTED_HTML_VIEW;
        } finally {
            closeQuietly(ewd);
        }
    }

    private static Predicate<Pair<String, String>> createFactorCriteria(final String ef, String efv) {
        if (isNullOrEmpty(ef)) {
            return alwaysTrue();
        } else if (isNullOrEmpty(efv)) {
            return firstEqualTo(ef);
        } else {
            return equalTo(create(ef, efv));
        }
    }

    private Predicate<Long> genePredicate(String gid) {
        if (isNullOrEmpty(gid))
            return alwaysTrue();

        return in(findGeneIds(Arrays.asList(gid.trim())));
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
     * @throws ResourceNotFoundException
     */
    private String getExperiment(Model model, String accession, @Nullable final String gid, @Nullable final String ef) throws ResourceNotFoundException {
        JsMapModel jsMapModel = JsMapModel.wrap(model);

        ExperimentPage page = createExperimentPage(accession);
        page.enhance(jsMapModel);

        jsMapModel
                .addJsAttribute("eid", page.getExperiment().getAccession())
                .addJsAttribute("gid", gid)
                .addJsAttribute("ef", ef);

        if (page.isExperimentInCuration()) {
            return "experimentpage/experiment-incuration";
        }

        return "experimentpage/experiment";
    }

    private class GeneToolTip {
        private final String geneName;
        private final Collection<String> geneIdentifiers;
        private final Collection<NameValuePair<String>> geneProperties;

        public GeneToolTip(final AtlasGene atlasGene) {
            this.geneName = atlasGene.getGeneName();

            final Map<String, Collection<String>> properties = atlasGene.getGeneProperties();
            this.geneIdentifiers = Collections2.transform(atlasProperties.getGeneAutocompleteNameFields(),
                    new Function<String, String>() {
                        public String apply(@Nonnull String geneProperty) {
                            return on(",").join(properties.get(geneProperty));
                        }
                    });

            final Map<String, String> curatedProperties = atlasProperties.getCuratedGeneProperties();
            this.geneProperties = Collections2.transform(atlasProperties.getGeneTooltipFields(),
                    new Function<String, NameValuePair<String>>() {
                        @Override
                        public NameValuePair<String> apply(@Nullable String input) {
                            return new NameValuePair<String>(
                                    curatedProperties.get(input),
                                    atlasGene.getPropertyValue(input));
                        }
                    });
        }

        @JsonProperty("name")
        public String getName() {
            return geneName;
        }

        @JsonProperty("identifiers")
        public String getIdentifiers() {
            return on(",").join(geneIdentifiers);
        }

        @JsonProperty("properties")
        public Collection<NameValuePair<String>> getProperties() {
            return geneProperties;
        }
    }

    private static class ExperimentTableRow {
        private final String geneName;
        private final String geneIdentifier;
        private final String deAccession;
        private final Integer deIndex;
        private final String factor;
        private final String factorValue;
        private final UpDownExpression upDown;
        private final String pValue;
        private final String tValue;

        public ExperimentTableRow(BestDesignElementsResult.Item item) {

            geneName = item.getGene().getGeneName();
            geneIdentifier = item.getGene().getGeneIdentifier();
            deAccession = item.getDeAccession();
            deIndex = item.getDeIndex();
            factor = item.getEf();
            factorValue = item.getEfv();
            pValue = formatPValue(item.getPValue());
            tValue = formatTValue(item.getTValue());
            upDown = UpDownExpression.valueOf(item.getPValue(), item.getTValue());
        }

        @JsonProperty("geneName")
        public String getGeneName() {
            return geneName;
        }

        @JsonProperty("geneIdentifier")
        public String getGeneIdentifier() {
            return geneIdentifier;
        }

        @JsonProperty("deAcc")
        public String getDeAccession() {
            return deAccession;
        }

        @JsonProperty("deIndex")
        public Integer getDeIndex() {
            return deIndex;
        }

        @JsonProperty("ef")
        public String getFactor() {
            return factor;
        }

        @JsonProperty("efv")
        public String getFactorValue() {
            return factorValue;
        }

        @JsonProperty("upDown")
        public String getUpDown() {
            return upDown.toString();
        }

        @JsonProperty("pVal")
        public String getPValue() {
            return pValue;
        }

        @JsonProperty("tVal")
        public String getTValue() {
            return tValue;
        }
    }

    private static Predicate<Pair<String, String>> firstEqualTo(final String s) {
        return new Predicate<Pair<String, String>>() {
            @Override
            public boolean apply(@Nullable Pair<String, String> input) {
                return input != null && s.equals(input.getFirst());
            }
        };
    }
}

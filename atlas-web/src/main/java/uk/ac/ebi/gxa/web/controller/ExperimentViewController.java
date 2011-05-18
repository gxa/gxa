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
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ucar.ma2.InvalidRangeException;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFDescriptor;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.web.ui.NameValuePair;
import uk.ac.ebi.gxa.web.ui.plot.AssayProperties;
import uk.ac.ebi.gxa.web.ui.plot.ExperimentPlot;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Predicates.alwaysTrue;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.netcdf.reader.NetCDFPredicates.containsAtLeastOneGene;
import static uk.ac.ebi.gxa.netcdf.reader.NetCDFPredicates.hasArrayDesign;
import static uk.ac.ebi.gxa.utils.NumberFormatUtil.formatPValue;
import static uk.ac.ebi.gxa.utils.NumberFormatUtil.formatTValue;

/**
 * @author Olga Melnichuk
 */
@Controller
public class ExperimentViewController extends ExperimentViewControllerBase {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewController.class);

    private final AtlasNetCDFDAO netCDFDAO;

    private final AtlasProperties atlasProperties;

    private final GeneSolrDAO geneSolrDAO;

    private final AtlasExperimentAnalyticsViewService experimentAnalyticsService;

    private final Function<String, String> curatedStringConverter = new Function<String, String>() {
        @Override
        public String apply(@Nullable String input) {
            String s = atlasProperties.getCuratedEf(input);
            return (s == null) ? input : s;
        }
    };

    @Autowired
    public ExperimentViewController(ExperimentSolrDAO solrDAO,
                                    AtlasDAO atlasDAO,
                                    AtlasNetCDFDAO netCDFDAO,
                                    AtlasProperties atlasProperties,
                                    GeneSolrDAO geneSolrDAO,
                                    AtlasExperimentAnalyticsViewService experimentAnalyticsService) {
        super(solrDAO, atlasDAO);
        this.netCDFDAO = netCDFDAO;
        this.atlasProperties = atlasProperties;
        this.geneSolrDAO = geneSolrDAO;
        this.experimentAnalyticsService = experimentAnalyticsService;
    }

    /**
     * An experiment page handler. If the experiment with the given id/accession exists it fills the model with the
     * appropriate values and returns the corresponding view. Parameters like gid (geneId) and ef (experiment factor)
     * are optional; they could be specified manually or by the url re-writer
     * (when url like /experiment/E-MTAB-62/ENSG00000136487/organism_part is used).
     *
     * @param accession an experiment accession to show experiment page for
     * @param gid       a gene identifier to fill in initial search fields
     * @param ef        an experiment factor name to fill in initial search fields
     * @param model     a model for the view to render
     * @return path of the view
     * @throws ResourceNotFoundException if an experiment with the given accession is not found
     */
    @RequestMapping(value = "/experiment", method = RequestMethod.GET)
    public String getExperiment(
            @RequestParam("eid") String accession,
            @RequestParam(value = "gid", required = false) String gid,
            @RequestParam(value = "ef", required = false) String ef,
            Model model) throws ResourceNotFoundException {

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

    /**
     * Returns experiment plots for given set of design elements.
     * (JSON view only supported)
     *
     * @param accession               an experiment accession to find out the required netCDF
     * @param adAcc                   an array design accession to find out the required netCDF
     * @param des                     an array of design element indexes to get plot data for
     * @param assayPropertiesRequired a boolean value to specify if assay properties ard needed
     * @param model                   a model for the view to render
     * @return the view path
     * @throws ResourceNotFoundException      if an experiment or array design is not found
     * @throws IOException                    if any netCDF file reading error happened
     * @throws ucar.ma2.InvalidRangeException if given design element indexes are out of range
     */
    @RequestMapping(value = "/experimentPlot", method = RequestMethod.GET)
    public String getExperimentPlot(
            @RequestParam("eid") String accession,
            @RequestParam("ad") String adAcc,
            @RequestParam("de") int[] des,
            @RequestParam(value = "assayPropertiesRequired", required = false, defaultValue = "false") Boolean assayPropertiesRequired,
            Model model
    ) throws ResourceNotFoundException, IOException, InvalidRangeException {

        ExperimentPage page = createExperimentPage(accession);
        if (page.getExperiment().getArrayDesign(adAcc) == null) {
            throw new ResourceNotFoundException("Improper array design accession: " + adAcc + " (in " + accession + " experiment)");
        }

        final Experiment experiment = atlasDAO.getExperimentByAccession(accession);
        NetCDFDescriptor proxyDescr = netCDFDAO.getNetCdfFile(experiment, hasArrayDesign(adAcc));
        model.addAttribute("plot", ExperimentPlot.create(des, proxyDescr, curatedStringConverter));
        if (assayPropertiesRequired) {
            model.addAttribute("assayProperties", AssayProperties.create(proxyDescr, curatedStringConverter));
        }
        return UNSUPPORTED_HTML_VIEW;
    }

    /**
     * Returns experiment table data for given search parameters.
     * (JSON view only supported)
     *
     * @param accession an experiment accession to find out the required netCDF
     * @param adAcc     an array design accession to find out the required netCDF
     * @param gid       a gene param to search with
     * @param ef        an experiment factor param to search with
     * @param efv       an experiment factor value param to search with
     * @param updown    an up/down condition to search with
     * @param offset    an offset of results to take
     * @param limit     a size of result set to take
     * @param model     a model for the view to render
     * @return the view path
     * @throws java.io.IOException if netCDF file could not be read
     */
    @RequestMapping(value = "/experimentTable", method = RequestMethod.GET)
    public String getExperimentTable(
            @RequestParam("eid") String accession,
            @RequestParam(value = "ad", required = false) String adAcc,
            @RequestParam(value = "gid", required = false) String gid,
            @RequestParam(value = "ef", required = false) String ef,
            @RequestParam(value = "efv", required = false) String efv,
            @RequestParam(value = "updown", required = false, defaultValue = "CONDITION_ANY") UpDownCondition updown,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit,
            Model model
    ) throws IOException {

        List<Long> geneIds = findGeneIds(gid);

        final Predicate<NetCDFProxy> ncdfPredicate;
        if (!isNullOrEmpty(adAcc)) {
            ncdfPredicate = hasArrayDesign(adAcc);
        } else if (!isNullOrEmpty(gid)) {
            ncdfPredicate = containsAtLeastOneGene(geneIds);
        } else {
            ncdfPredicate = alwaysTrue();
        }

        final Experiment experiment = atlasDAO.getExperimentByAccession(accession);
        NetCDFDescriptor ncdfDescr = netCDFDAO.getNetCdfFile(experiment, ncdfPredicate);

        final BestDesignElementsResult res = (ncdfDescr == null) ?
                BestDesignElementsResult.empty() :
                experimentAnalyticsService.findBestGenesForExperiment(
                        ncdfDescr,
                        geneIds,
                        isNullOrEmpty(ef) ? Collections.<String>emptyList() : Arrays.asList(ef),
                        isNullOrEmpty(efv) ? Collections.<String>emptyList() : Arrays.asList(efv),
                        updown,
                        offset,
                        limit);

        model.addAttribute("arrayDesign", getArrayDesignAccession(ncdfDescr));
        model.addAttribute("totalSize", res.getTotalSize());
        model.addAttribute("items", Iterables.transform(res,
                new Function<BestDesignElementsResult.Item, ExperimentTableRow>() {
                    public ExperimentTableRow apply(@Nonnull BestDesignElementsResult.Item item) {
                        return new ExperimentTableRow(item);
                    }
                })
        );
        model.addAttribute("geneToolTips", getGeneTooltips(res.getGenes()));
        return UNSUPPORTED_HTML_VIEW;
    }

    private Map<String, GeneToolTip> getGeneTooltips(Collection<AtlasGene> genes) {
        Map<String, GeneToolTip> tips = new HashMap<String, GeneToolTip>(genes.size());
        for (AtlasGene gene : genes) {
            tips.put("" + gene.getGeneId(), new GeneToolTip(gene));
        }
        return tips;
    }

    private String getArrayDesignAccession(NetCDFDescriptor descr) throws IOException {
        if (descr == null) {
            return null;
        }

        NetCDFProxy proxy = null;
        try {
            proxy = descr.createProxy();
            return proxy.getArrayDesignAccession();
        } finally {
            closeQuietly(proxy);
        }
    }

    private List<Long> findGeneIds(String... query) {
        List<Long> genes = Lists.newArrayList();

        for (String text : query) {
            if (Strings.isNullOrEmpty(text)) {
                continue;
            }
            GeneSolrDAO.AtlasGeneResult res = geneSolrDAO.getGeneByIdentifier(text);
            if (!res.isFound()) {
                for (AtlasGene gene : geneSolrDAO.getGenesByName(text)) {
                    genes.add((long) gene.getGeneId());
                }
            } else {
                genes.add((long) res.getGene().getGeneId());
            }
        }
        return genes;
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
}

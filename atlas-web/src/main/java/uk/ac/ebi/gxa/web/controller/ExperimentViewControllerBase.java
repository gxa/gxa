package uk.ac.ebi.gxa.web.controller;

import ae3.model.AtlasExperiment;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.service.experiment.ExperimentDataService;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Olga Melnichuk
 *         Date: 18/03/2011
 */
class ExperimentViewControllerBase extends AtlasViewController {
    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewControllerBase.class);

    private final ExperimentDataService expDataService;
    private final AtlasProperties atlasProperties;

    ExperimentViewControllerBase(ExperimentDataService expDataService, AtlasProperties atlasProperties) {
        this.expDataService = expDataService;
        this.atlasProperties = atlasProperties;
    }

    protected ExperimentPage createExperimentPage(String expAccession, String gid) throws RecordNotFoundException {
        AtlasExperiment exp = expDataService.getExperimentFromSolr(expAccession);

        String expressionAtlasLink = null;
        if (atlasProperties.getDifferentialExpressionAtlasExperiments().contains(expAccession)) {
            expressionAtlasLink = atlasProperties.getDifferentialExpressionAtlasLink() + expAccession + (!Strings.isNullOrEmpty(gid) ? "?geneQuery=" + gid : "");
        }
        boolean hideGxaContent = false;
        if (atlasProperties.getHideGxaContentExperiments().contains(expAccession)) {
            hideGxaContent = true;
        }
        boolean enableRDFLink = false;
        if (atlasProperties.isRDFExperimentLinkEnabled()) {
            enableRDFLink = true;
        }
        String rdfExperimentLink = null;
        if (!Strings.isNullOrEmpty(atlasProperties.getRDFExperimentLink())) {
            rdfExperimentLink = atlasProperties.getRDFExperimentLink();
        }

        return new ExperimentPage(
                exp,
                exp.getExperiment().isRNASeq(),
                exp.getExperiment().getSpecies(),
                Collections2.transform(exp.getExperiment().getArrayDesigns(),
                        new Function<ArrayDesign, String>() {
                            public String apply(@Nonnull ArrayDesign ad) {
                                return ad.getAccession();
                            }
                        }),
                expressionAtlasLink,
                hideGxaContent, enableRDFLink, rdfExperimentLink);
    }

    public static class ExperimentPage {
        private final AtlasExperiment exp;
        private final boolean rnaSeq;
        private final List<String> species = new ArrayList<String>();
        private final List<String> arrayDesigns = Lists.newArrayList();
        private final String expressionAtlasLink;
        private final boolean hideGxaContent;
        private final boolean enableRDFLink;
        private final String rdfExperimentLink;

        public ExperimentPage(AtlasExperiment exp, boolean rnaSeq, List<String> species, Collection<String> arrayDesigns,
                              String expressionAtlasLink, boolean hideGxaContent, boolean enableRDFLink, String rdfExperimentLink) {
            this.exp = exp;
            this.rnaSeq = rnaSeq;
            this.species.addAll(species);
            this.arrayDesigns.addAll(arrayDesigns);
            this.expressionAtlasLink = expressionAtlasLink;
            this.hideGxaContent = hideGxaContent;
            this.enableRDFLink = enableRDFLink;
            this.rdfExperimentLink = rdfExperimentLink;
        }

        public void enhance(Model model) {
            model.addAttribute("exp", exp)
                    .addAttribute("expSpecies", species)
                    .addAttribute("isRNASeq", rnaSeq)
                    .addAttribute("expressionAtlasLink", expressionAtlasLink)
                    .addAttribute("hideGxaContent", hideGxaContent)
                    .addAttribute("enableRDFLink", enableRDFLink)
                    .addAttribute("rdfExperimentLink", rdfExperimentLink);
        }

        public boolean isExperimentInCuration() {
            return exp.getExperimentFactors().isEmpty();
        }

        public List<String> getArrayDesigns() {
            return arrayDesigns;
        }

        Experiment getExperiment() {
            return exp.getExperiment();
        }
    }
}

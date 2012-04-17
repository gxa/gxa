package uk.ac.ebi.gxa.web.controller;

import ae3.model.AtlasExperiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.service.experiment.ExperimentDataService;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Olga Melnichuk
 *         Date: 18/03/2011
 */
class ExperimentViewControllerBase extends AtlasViewController {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewControllerBase.class);

    private final ExperimentDataService expDataService;

    ExperimentViewControllerBase(ExperimentDataService expDataService) {
        this.expDataService = expDataService;
    }

    protected ExperimentPage createExperimentPage(String expAccession) throws RecordNotFoundException {
        AtlasExperiment exp = expDataService.getExperimentFromSolr(expAccession);

        return new ExperimentPage(
                exp,
                exp.getExperiment().isRNASeq(),
                exp.getExperiment().getSpecies()
        );
    }

    public static class ExperimentPage {
        private final AtlasExperiment exp;
        private final boolean rnaSeq;
        private final List<String> species = new ArrayList<String>();

        public ExperimentPage(AtlasExperiment exp, boolean rnaSeq, List<String> species) {
            this.exp = exp;
            this.rnaSeq = rnaSeq;
            this.species.addAll(species);
        }

        public void enhance(Model model) {
            model.addAttribute("exp", exp)
                    .addAttribute("expSpecies", species)
                    .addAttribute("isRNASeq", rnaSeq);
        }

        public boolean isExperimentInCuration() {
            return exp.getExperimentFactors().isEmpty();
        }

        Experiment getExperiment() {
            return exp.getExperiment();
        }
    }
}

package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import ae3.model.AtlasExperiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Olga Melnichuk
 *         Date: 18/03/2011
 */
class ExperimentViewControllerBase extends AtlasViewController {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewControllerBase.class);

    protected final ExperimentSolrDAO experimentSolrDAO;
    protected final AtlasDAO atlasDAO;

    public ExperimentViewControllerBase(ExperimentSolrDAO experimentSolrDAO, AtlasDAO atlasDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
        this.atlasDAO = atlasDAO;
    }

    protected ExperimentPage createExperimentPage(String expAccession) throws ResourceNotFoundException {
        AtlasExperiment exp = getExperimentByAccession(expAccession);

        return new ExperimentPage(
                exp,
                exp.getExperiment().isRNASeq(),
                exp.getExperiment().getSpecies()
        );
    }

    protected AtlasExperiment getExperimentByAccession(String accession) throws ResourceNotFoundException {
        final AtlasExperiment exp = experimentSolrDAO.getExperimentByAccession(accession);

        if (exp == null) {
            throw new ResourceNotFoundException("There are no records for experiment " + accession);
        }
        return exp;
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

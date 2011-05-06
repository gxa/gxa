package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import ae3.model.AtlasExperimentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;

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
        AtlasExperimentImpl exp = getExperimentByAccession(expAccession);

        return new ExperimentPage(
                exp,
                isRNASeq(exp.getExperiment()),
                getSpecies(exp.getExperiment())
        );
    }

    protected List<String> getSpecies(Experiment exp) {
        return atlasDAO.getSpeciesForExperiment(exp.getId());
    }

    protected boolean isRNASeq(Experiment exp) {
        // TODO: see ticket #2706
        for (Assay assay : exp.getAssays()) {
            ArrayDesign design = assay.getArrayDesign();
            String designType = design == null ? "" : design.getType();
            if (designType != null && designType.indexOf("virtual") >= 0) {
                return true;
            }
        }
        return false;
    }

    protected AtlasExperimentImpl getExperimentByAccession(String accession) throws ResourceNotFoundException {
        final AtlasExperimentImpl exp = experimentSolrDAO.getExperimentByAccession(accession);

        if (exp == null) {
            throw new ResourceNotFoundException("There are no records for experiment " + accession);
        }
        return exp;
    }

    public static class ExperimentPage {
        private final AtlasExperimentImpl exp;
        private final boolean rnaSeq;
        private final List<String> species = new ArrayList<String>();

        public ExperimentPage(AtlasExperimentImpl exp, boolean rnaSeq, List<String> species) {
            this.exp = exp;
            this.rnaSeq = rnaSeq;
            this.species.addAll(species);
        }

        public AtlasExperimentImpl getExp() {
            return exp;
        }

        public void enhance(Model model) {
            model.addAttribute("exp", exp)
                    .addAttribute("expSpecies", species)
                    .addAttribute("isRNASeq", rnaSeq);
        }

        public boolean isExperimentInCuration() {
            return exp.getExperimentFactors().isEmpty();
        }
    }
}

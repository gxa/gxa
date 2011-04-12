package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import ae3.model.AtlasExperimentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.gxa.Experiment;

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

    protected ExperimentPage createExperimentPage(String expAccession, String ad) throws ResourceNotFoundException {
        Experiment exp = getExperimentByAccession(expAccession);

        return new ExperimentPage(
                exp,
                ((AtlasExperimentImpl)exp).getArrayDesign(ad),
                isRNASeq(exp),
                getSpecies(exp)
        );
    }

    protected List<String> getSpecies(Experiment exp) {
        return atlasDAO.getSpeciesForExperiment(exp.getId());
    }

    protected boolean isRNASeq(Experiment exp) {
        // TODO: see ticket #2706
        boolean isRNASeq = Boolean.FALSE;
        for (String adAcc : ((AtlasExperimentImpl)exp).getArrayDesigns()) {
            ArrayDesign design = atlasDAO.getArrayDesignShallowByAccession(adAcc);
            String designType = design == null ? "" : design.getType();
            isRNASeq = isRNASeq || (designType != null && designType.indexOf("virtual") >= 0);
        }
        return isRNASeq;
    }

    protected Experiment getExperimentByAccession(String accession) throws ResourceNotFoundException {
        final Experiment exp = experimentSolrDAO.getExperimentByAccession(accession);

        if (exp == null) {
            throw new ResourceNotFoundException("There are no records for experiment " + accession);
        }
        return exp;
    }

    public static class ExperimentPage {
        private final Experiment exp;
        private final String arrayDesign;
        private final boolean rnaSeq;
        private final List<String> species = new ArrayList<String>();

        public ExperimentPage(Experiment exp, String arrayDesign, boolean rnaSeq, List<String> species) {
            this.exp = exp;
            this.arrayDesign = arrayDesign;
            this.rnaSeq = rnaSeq;
            this.species.addAll(species);
        }

        public Experiment getExp() {
            return exp;
        }

        public void enhance(Model model) {
            model.addAttribute("exp", exp)
                    .addAttribute("expSpecies", species)
                    .addAttribute("eid", exp.getId())

                    .addAttribute("arrayDesigns", ((AtlasExperimentImpl)exp).getArrayDesigns())
                    .addAttribute("arrayDesign", arrayDesign)
                    .addAttribute("isRNASeq", rnaSeq);

        }

        public boolean isExperimentInCuration() {
            return ((AtlasExperimentImpl)exp).getExperimentFactors().isEmpty();
        }
    }
}

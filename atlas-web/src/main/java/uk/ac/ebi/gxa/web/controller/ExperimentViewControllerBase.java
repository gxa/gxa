package uk.ac.ebi.gxa.web.controller;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Olga Melnichuk
 *         Date: 18/03/2011
 */
class ExperimentViewControllerBase {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentViewControllerBase.class);

    protected final AtlasSolrDAO atlasSolrDAO;
    protected final AtlasDAO atlasDAO;

    public ExperimentViewControllerBase(AtlasSolrDAO atlasSolrDAO, AtlasDAO atlasDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
        this.atlasDAO = atlasDAO;
    }

    protected ExperimentPage createExperimentPage(String expAccession, String ad) throws ResourceNotFoundException {
        AtlasExperiment exp = getExperimentByAccession(expAccession);

        return new ExperimentPage(
                exp,
                exp.getArrayDesign(ad),
                isRNASeq(exp),
                getSpecies(exp)
        );
    }

    protected List<String> getSpecies(AtlasExperiment exp) {
        return atlasDAO.getSpeciesForExperiment(exp.getId().longValue());
    }

    protected boolean isRNASeq(AtlasExperiment exp) {
        // TODO: see ticket #2706
        boolean isRNASeq = Boolean.FALSE;
        for (String adAcc : exp.getArrayDesigns()) {
            ArrayDesign design = atlasDAO.getArrayDesignShallowByAccession(adAcc);
            String designType = design == null ? "" : design.getType();
            isRNASeq = isRNASeq || (designType != null && designType.indexOf("virtual") >= 0);
        }
        return isRNASeq;
    }

    protected AtlasExperiment getExperimentByAccession(String accession) throws ResourceNotFoundException {
        final AtlasExperiment exp = atlasSolrDAO.getExperimentByAccession(accession);

        if (exp == null) {
            throw new ResourceNotFoundException("There are no records for experiment " + accession);
        }
        return exp;
    }

    public static class ExperimentPage {
        private final AtlasExperiment exp;
        private final String arrayDesign;
        private final boolean rnaSeq;
        private final List<String> species = new ArrayList<String>();

        public ExperimentPage(AtlasExperiment exp, String arrayDesign, boolean rnaSeq, List<String> species) {
            this.exp = exp;
            this.arrayDesign = arrayDesign;
            this.rnaSeq = rnaSeq;
            this.species.addAll(species);
        }

        public AtlasExperiment getExp() {
            return exp;
        }

        public void enhance(Model model) {
            model.addAttribute("exp", exp)
                    .addAttribute("expSpecies", species)
                    .addAttribute("eid", exp.getId())

                    .addAttribute("arrayDesigns", exp.getArrayDesigns())
                    .addAttribute("arrayDesign", arrayDesign)
                    .addAttribute("isRNASeq", rnaSeq);

        }

        public boolean isExperimentInCuration() {
            return exp.getExperimentFactors().isEmpty();
        }
    }
}

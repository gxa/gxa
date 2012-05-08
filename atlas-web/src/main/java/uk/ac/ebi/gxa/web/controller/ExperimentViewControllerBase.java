package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import ae3.model.AtlasExperiment;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
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

    private final ExperimentSolrDAO experimentSolrDAO;
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
                exp.getExperiment().getSpecies(),
                Collections2.transform(exp.getExperiment().getArrayDesigns(),
                        new Function<ArrayDesign, String>() {
                            public String apply(@Nonnull ArrayDesign ad) {
                                return ad.getAccession();
                            }
                        }));
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
        private final List<String> arrayDesigns = Lists.newArrayList();

        public ExperimentPage(AtlasExperiment exp, boolean rnaSeq, List<String> species, Collection<String> arrayDesigns) {
            this.exp = exp;
            this.rnaSeq = rnaSeq;
            this.species.addAll(species);
            this.arrayDesigns.addAll(arrayDesigns);
        }

        public void enhance(Model model) {
            model.addAttribute("exp", exp)
                    .addAttribute("expSpecies", species)
                    .addAttribute("isRNASeq", rnaSeq);
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

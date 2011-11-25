package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import ae3.dao.GeneSolrDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;

/**
 * @author Olga Melnichuk
 *         Date: 15/03/2011
 */
@Controller
public class ExperimentDesignViewController extends ExperimentViewControllerBase {

    @Autowired
    public ExperimentDesignViewController(ExperimentSolrDAO experimentSolrDAO, GeneSolrDAO geneSolrDAO, AtlasDAO atlasDAO) {
        super(experimentSolrDAO, geneSolrDAO, atlasDAO);
    }

    @RequestMapping(value = "/experimentDesign", method = RequestMethod.GET)
    public String getExperimentDesign(
            @RequestParam("eid") String accession,
            Model model) throws ResourceNotFoundException, AtlasDataException {

        ExperimentPage expPage = createExperimentPage(accession);
        expPage.enhance(model);

        model.addAttribute("experimentDesign", new ExperimentDesignUI(expPage.getExperiment()));
        return "experimentpage/experiment-design";
    }
}

package uk.ac.ebi.gxa.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.service.experiment.ExperimentDataService;

/**
 * @author Olga Melnichuk
 *         Date: 15/03/2011
 */
@Controller
public class ExperimentDesignViewController extends ExperimentViewControllerBase {

    @Autowired
    public ExperimentDesignViewController(ExperimentDataService expDataService, AtlasProperties atlasProperties) {
        super(expDataService, atlasProperties);
    }

    @RequestMapping(value = "/experimentDesign", method = RequestMethod.GET)
    public String getExperimentDesign(
            @RequestParam("eacc") String accession,
            Model model) throws RecordNotFoundException, AtlasDataException {

        ExperimentPage expPage = createExperimentPage(accession, null);
        expPage.enhance(model);
        return "experimentpage/experiment-design";
    }

    @RequestMapping(value = "/experimentDesignTable", method = RequestMethod.GET)
    public String getExperimentDesignTable(
            @RequestParam("eacc") String accession,
            @RequestParam(value = "limit", required = false, defaultValue = "-1") int limit,
            @RequestParam(value = "offset", required = false, defaultValue = "-1") int offset,
            Model model) throws RecordNotFoundException, AtlasDataException {

        ExperimentPage expPage = createExperimentPage(accession, null);
        model.addAttribute("experimentDesign", new ExperimentDesignUI(expPage.getExperiment(), offset, limit));
        return "experimentDesignTable";
    }
}

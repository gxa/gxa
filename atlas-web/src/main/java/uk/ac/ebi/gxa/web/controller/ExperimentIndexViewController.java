package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Alexey Filippov
 */
@Controller
public class ExperimentIndexViewController extends AtlasViewController {

    private final ExperimentSolrDAO experimentSolrDAO;

    @Autowired
    public ExperimentIndexViewController(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }

    @RequestMapping(value = "/experimentIndex", method = RequestMethod.GET)
    public String getGeneIndex(@RequestParam(value = "start", defaultValue = "0") int start,
                               @RequestParam(value = "count", defaultValue = "10") int count, Model model) {

        ExperimentSolrDAO.AtlasExperimentsResult experiments =
                experimentSolrDAO.getExperimentsByQuery("*:*", start, count);
        model.addAttribute("experiments", experiments.getExperiments());
        model.addAttribute("total", experiments.getTotalResults());
        model.addAttribute("start", start);
        model.addAttribute("count", count);
        return "experimentpage/experiment-index";
    }
}

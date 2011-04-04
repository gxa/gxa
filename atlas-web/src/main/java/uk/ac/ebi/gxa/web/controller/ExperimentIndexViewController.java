package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import org.apache.solr.client.solrj.SolrQuery;
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
                               @RequestParam(value = "count", defaultValue = "10") int count,
                               @RequestParam(value = "sort", defaultValue = "accession") String sort,
                               @RequestParam(value = "dir", defaultValue = "asc") String dir,
                               Model model) {

        ExperimentSolrDAO.AtlasExperimentsResult experiments =
                experimentSolrDAO.getExperimentsByQuery("*:*", start, count, sort, SolrQuery.ORDER.valueOf(dir));
        model.addAttribute("experiments", experiments.getExperiments());
        model.addAttribute("total", experiments.getTotalResults());
        model.addAttribute("start", start);
        model.addAttribute("count", count);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "experimentpage/experiment-index";
    }
}

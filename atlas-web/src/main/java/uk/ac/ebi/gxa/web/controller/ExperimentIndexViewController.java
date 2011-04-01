package uk.ac.ebi.gxa.web.controller;

import ae3.dao.AtlasSolrDAO;
import ae3.model.AtlasExperiment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author Olga Melnichuk
 *         Date: 18/03/2011
 */
@Controller
public class ExperimentIndexViewController extends AtlasViewController {

    private final AtlasSolrDAO atlasSolrDAO;

    @Autowired
    public ExperimentIndexViewController(AtlasSolrDAO atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    @RequestMapping(value = "/experimentIndex", method = RequestMethod.GET)
    public String getGeneIndex(@RequestParam(value = "start", defaultValue = "0") int start,
                               @RequestParam(value = "count", defaultValue = "10") int count, Model model) {
        List<AtlasExperiment> experiments = copyOf(atlasSolrDAO.getExperiments());
        int fromIndex = max(min(start, experiments.size() - 1), 0);
        int toIndex = min(fromIndex + count, experiments.size());
        model.addAttribute("experiments", experiments.subList(fromIndex, toIndex));
        model.addAttribute("total", experiments.size());
        model.addAttribute("start", fromIndex);
        model.addAttribute("count", count);
        return "experimentpage/experiment-index";
    }
}

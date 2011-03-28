package uk.ac.ebi.gxa.web.controller;

import ae3.dao.AtlasSolrDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
    public String getGeneIndex(Model model) {
        model.addAttribute("allexpts", atlasSolrDAO.getExperiments());
        return "experimentpage/experiment-index";
    }
}

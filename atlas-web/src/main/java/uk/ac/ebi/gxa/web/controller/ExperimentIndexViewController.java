package uk.ac.ebi.gxa.web.controller;

import ae3.dao.ExperimentSolrDAO;
import org.apache.solr.client.solrj.SolrQuery;
import org.displaytag.tags.TableTagParameters;
import org.displaytag.util.ParamEncoder;
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
    public static final int PAGE_SIZE = 20;

    private final ExperimentSolrDAO experimentSolrDAO;

    @Autowired
    public ExperimentIndexViewController(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }

    @RequestMapping(value = "/experimentIndex", method = RequestMethod.GET)
    public String getGeneIndex(@RequestParam(value = "d-2529291-p", defaultValue = "0") int page,
                               @RequestParam(value = "d-2529291-s", defaultValue = "accession") String sort,
                               @RequestParam(value = "d-2529291-o", defaultValue = "1") int dir,
                               Model model) {
        assert "d-2529291-p".equals(new ParamEncoder("experiment").encodeParameterName(TableTagParameters.PARAMETER_PAGE));
        assert "d-2529291-s".equals(new ParamEncoder("experiment").encodeParameterName(TableTagParameters.PARAMETER_SORT));
        assert "d-2529291-o".equals(new ParamEncoder("experiment").encodeParameterName(TableTagParameters.PARAMETER_ORDER));

        ExperimentSolrDAO.AtlasExperimentsResult experiments =
                experimentSolrDAO.getExperimentsByQuery("*:*",
                        page * PAGE_SIZE, PAGE_SIZE, sort, displayTagSortToSolr(dir));
        model.addAttribute("experiments", experiments.getExperiments());
        model.addAttribute("total", experiments.getTotalResults());
        model.addAttribute("start", page * PAGE_SIZE);
        model.addAttribute("count", PAGE_SIZE);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        return "experimentpage/experiment-index";
    }

    private static SolrQuery.ORDER displayTagSortToSolr(int dir) {
        return dir == 1 ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
    }
}

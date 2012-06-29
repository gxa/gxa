package uk.ac.ebi.gxa.web.controller;

import ae3.dao.DAOException;
import ae3.dao.ExperimentSolrDAO;
import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.displaytag.tags.TableTagParameters;
import org.displaytag.util.ParamEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.annotation.Nullable;
import java.util.ArrayList;

import static com.google.common.collect.Lists.transform;

/**
 * @author Alexey Filippov
 */
@Controller
public class ExperimentIndexViewController extends AtlasViewController {
    public static final int PAGE_SIZE = 20;

    // TODO: 4alf: migrate it to ExperimentDAO?
    private final ExperimentSolrDAO experimentSolrDAO;

    private static final String SEARCH_ALL = "*:*";

    @Autowired
    public ExperimentIndexViewController(ExperimentSolrDAO experimentSolrDAO) {
        this.experimentSolrDAO = experimentSolrDAO;
    }

    @RequestMapping(value = "/experimentIndex", method = RequestMethod.GET)
    public String getGeneIndex(@RequestParam(value = "q", defaultValue = SEARCH_ALL) String query,
                               @RequestParam(value = PAGE_PARAM, defaultValue = "1") int page,
                               @RequestParam(value = SORT_PARAM, defaultValue = "loaddate") String sort,
                               @RequestParam(value = DIR_PARAM, defaultValue = "2") int dir,
                               Model model) {

        ExperimentSolrDAO.AtlasExperimentsResult experiments = null;
        try {
            experiments = experimentSolrDAO.getExperimentsByQuery(queryForSearch(query),
                    (page - 1) * PAGE_SIZE, PAGE_SIZE, sort, displayTagSortToSolr(dir));
        } catch (DAOException e) {
            return invalidQuery(query, model);
        }

        if (experiments.getTotalResults() == 1) {
            return singleResult(experiments);
        }

        model.addAttribute("experiments", transform(experiments.getExperiments(), new Function<Experiment, ExperimentIndexLine>() {
            @Override
            public ExperimentIndexLine apply(@Nullable Experiment experiment) {
                return new ExperimentIndexLine(experiment);
            }
        }));
        model.addAttribute("total", experiments.getTotalResults());
        model.addAttribute("count", PAGE_SIZE);
        model.addAttribute("query", queryForView(query));
        model.addAttribute("invalidquery", 0);
        return "experimentpage/experiment-index";
    }

    private String singleResult(ExperimentSolrDAO.AtlasExperimentsResult experiments) {
        return "redirect:/experiment/" + experiments.getExperiments().get(0).getAccession();
    }

    private String invalidQuery(String query, Model model) {
        model.addAttribute("invalidquery", 1);
        model.addAttribute("query", queryForView(query));
        model.addAttribute("experiments", new ArrayList<ExperimentIndexLine>());
        model.addAttribute("total", 0);
        model.addAttribute("count", PAGE_SIZE);
        return "experimentpage/experiment-index";
    }

    private static SolrQuery.ORDER displayTagSortToSolr(int dir) {
        return dir == 1 ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc;
    }

    /*
    The display:tag is sort of a strange library, while the best I could find.
    One of its peculiarities is, it encodes its parameters - in order to avoid clashes, perhaps.
    So on one hand, we didn't want to rewrite the control, on the other hand, the approach described
    at http://displaytag.sourceforge.net/1.2/tut_externalSortAndPage.html is a bit too verbose.
    Hence, we map parameters as we would in Spring world, and verify the worlds are still connected here.
    */
    private static final String PAGE_PARAM = "d-2529291-p";
    private static final String SORT_PARAM = "d-2529291-s";
    private static final String DIR_PARAM = "d-2529291-o";

    static {
        ParamEncoder encoder = new ParamEncoder("experiment");
        assert PAGE_PARAM.equals(encoder.encodeParameterName(TableTagParameters.PARAMETER_PAGE));
        assert SORT_PARAM.equals(encoder.encodeParameterName(TableTagParameters.PARAMETER_SORT));
        assert DIR_PARAM.equals(encoder.encodeParameterName(TableTagParameters.PARAMETER_ORDER));
    }

    private static String queryForSearch(String query) {
        if (StringUtils.EMPTY.equals(query)) {
            query = SEARCH_ALL;
        }
        return query;
    }

    private static String queryForView(String query) {
        if (SEARCH_ALL.equals(query)) {
            query = StringUtils.EMPTY;
        }
        return query;
    }

//    private void processInvalidQuery(){
//        model.addAttribute("invalidquery", 1);
//        model.addAttribute("total", experiments.getTotalResults());
//        model.addAttribute("count", PAGE_SIZE);
//        model.addAttribute("query", queryForView(query));
//        model.addAttribute("invalidquery", 0);
//    }
}

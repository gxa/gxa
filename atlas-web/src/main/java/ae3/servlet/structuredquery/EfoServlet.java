package ae3.servlet.structuredquery;

import ae3.restresult.RestOut;
import ae3.service.structuredquery.AtlasEfoService;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author pashky
 */
public class EfoServlet extends RestServlet {
    public static class Result {
        private Collection hls;
        private Collection res;

        public Result(Collection hls, Collection res) {
            this.hls = hls;
            this.res = res;
        }

        public @RestOut Collection hl() {
            return hls;
        }

        public @RestOut(xmlItemName = "efo") Collection tree() {
            return res;
        }

    }

    private AtlasEfoService efoService;

    public AtlasEfoService getEfoService() {
        return efoService;
    }

    public void setEfoService(AtlasEfoService efoService) {
        this.efoService = efoService;
    }

    public Object process(HttpServletRequest request) {

        Collection<AtlasEfoService.EfoTermCount> result = null;
        String id = request.getParameter("childrenOf");

        if (id != null) {
            if (id.length() == 0) {
                id = null;
            }
            log.info("EFO request for children of " + id);
            result = efoService.getTermChildren(id);
        }
        else {
            id = request.getParameter("downTo");
            if (id != null && id.length() != 0) {
                result = efoService.getTreeDownToTerm(id);
                log.info("EFO request for tree down to " + id);
            }
            else if (id != null && id.length() == 0) {
                // just show roots if nothing is down to
                log.info("EFO request for tree root");
                result = efoService.getTermChildren(null);
            }
            else {
                id = request.getParameter("parentsOf");
                if (id != null && id.length() != 0) {
                    log.info("EFO request for parents of " + id);
                    result = new ArrayList<AtlasEfoService.EfoTermCount>();
                    for (List<AtlasEfoService.EfoTermCount> i : efoService.getTermParentPaths(id)) {
                        result.addAll(i);
                    }
                }

            }
        }


        final String highlights = request.getParameter("hl");
        final Collection hls =
                highlights != null && highlights.length() != 0
                        ? efoService.searchTerms(EscapeUtil.parseQuotedList(highlights)) : null;

        return new Result(hls, result);
    }
}

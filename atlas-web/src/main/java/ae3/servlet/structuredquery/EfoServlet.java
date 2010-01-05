package ae3.servlet.structuredquery;

import ae3.restresult.RestOut;
import ae3.service.structuredquery.AtlasEfoService;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
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

    public Object process(HttpServletRequest request) {
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        AtlasEfoService service = (AtlasEfoService)context.getBean("atlasEfoService");


        Collection<AtlasEfoService.EfoTermCount> result = null;
        String id = request.getParameter("childrenOf");

        if (id != null) {
            if (id.length() == 0) {
                id = null;
            }
            log.info("EFO request for children of " + id);
            result = service.getTermChildren(id);
        }
        else {
            id = request.getParameter("downTo");
            if (id != null && id.length() != 0) {
                result = service.getTreeDownToTerm(id);
                log.info("EFO request for tree down to " + id);
            }
            else if (id != null && id.length() == 0) {
                // just show roots if nothing is down to
                log.info("EFO request for tree root");
                result = service.getTermChildren(null);
            }
            else {
                id = request.getParameter("parentsOf");
                if (id != null && id.length() != 0) {
                    log.info("EFO request for parents of " + id);
                    result = new ArrayList<AtlasEfoService.EfoTermCount>();
                    for (List<AtlasEfoService.EfoTermCount> i : service.getTermParentPaths(id)) {
                        result.addAll(i);
                    }
                }

            }
        }


        final String highlights = request.getParameter("hl");
        final Collection hls =
                highlights != null && highlights.length() != 0
                        ? service.searchTerms(EscapeUtil.parseQuotedList(highlights)) : null;

        return new Result(hls, result);
    }
}

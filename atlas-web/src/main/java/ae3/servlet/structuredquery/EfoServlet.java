package ae3.servlet.structuredquery;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import ae3.service.structuredquery.EfoValueListHelper;
import ae3.util.EscapeUtil;
import org.json.JSONObject;

/**
 * @author pashky
 */
public class EfoServlet extends HttpServlet {
    private Logger log = LoggerFactory.getLogger(getClass());

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    private void doIt(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");

        EfoValueListHelper service = ae3.service.ArrayExpressSearchService.instance()
                .getStructQueryService().getEfoListHelper();

        String id = request.getParameter("id");
        if(id != null && id.length() == 0)
            id = null;

        String downTo = null;
        if(id == null) {
            downTo = request.getParameter("downTo");
            if(downTo != null && downTo.length() == 0)
                downTo = null;
        }

        String highlights = request.getParameter("hl");
        if(highlights != null && highlights.length() == 0)
            highlights = null;

        log.info("EFO request for <" + id + "> expand down to <" + downTo + ">");

        try {
            JSONObject o = new JSONObject();
            o.putOpt("hl", highlights != null ? new JSONArray(service.searchTerms(EscapeUtil.parseQuotedList(highlights)), false) : null);
            o.put("tree", new JSONArray(downTo == null ? service.getTermChildren(id) : service.getTreeDownToTerm(downTo), false));
            o.write(response.getWriter());
        } catch(JSONException e) {
            log.error("JSON expception occured", e);
        }
    }
}

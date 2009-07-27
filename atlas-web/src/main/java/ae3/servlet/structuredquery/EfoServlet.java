package ae3.servlet.structuredquery;

import ae3.service.structuredquery.EfoValueListHelper;
import ae3.util.EscapeUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pashky
 */
public class EfoServlet extends JsonServlet {
    public JSONObject process(HttpServletRequest request) throws JSONException {
        EfoValueListHelper service = ae3.service.ArrayExpressSearchService.instance()
                .getStructQueryService().getEfoListHelper();


        Collection result = null;
        String id = request.getParameter("childrenOf");

        //AZ:shortcut to make drop down show something first time 
        if((id == null)&&("".equals(request.getParameter("downTo"))))
            id="EFO_0000001";

        if(id != null && id.length() != 0)
            result = service.getTermChildren(id);
        else {
            id = request.getParameter("downTo");
            if(id != null && id.length() != 0)
                result = service.getTreeDownToTerm(id);
            else {
                id = request.getParameter("parentsOf");
                if(id != null && id.length() != 0) {
                    result = new ArrayList();
                    for(List<EfoValueListHelper.EfoTermCount> i : service.getTermParentPaths(id))
                        result.addAll(i);
                }

            }
        }

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

        JSONObject o = new JSONObject();
        o.putOpt("hl", highlights != null ? new JSONArray(service.searchTerms(EscapeUtil.parseQuotedList(highlights)), false) : null);
        if(result != null)
            o.put("tree", new JSONArray(result, false));

        return o;
    }
}

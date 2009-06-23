package ae3.servlet.structuredquery;

import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.service.structuredquery.AutoCompleteItem;
import ae3.service.structuredquery.IValueListHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pashky
 */
public class FactorValuesServlet extends JsonServlet {

    public JSONObject process(HttpServletRequest request) throws JSONException {

        AtlasStructuredQueryService service = ae3.service.ArrayExpressSearchService.instance()
                .getStructQueryService();
        
        List<IValueListHelper> listers = new ArrayList<IValueListHelper>();

        String type = request.getParameter("type");
        if("gene".equals(type)) {
            listers.add(service.getGeneListHelper());
        } else if("efv".equals(type)) {
            listers.add(service.getEfvListHelper());
        } else if("efo".equals(type)) {
            listers.add(service.getEfoListHelper());
        } else if("efoefv".equals(type)) {
            listers.add(service.getEfoListHelper());
            listers.add(service.getEfvListHelper());
        }

        JSONObject result = new JSONObject();

        String factor = request.getParameter("factor");
        result.put("factor", factor);

        if("all".equals(request.getParameter("mode")))
        {

            List<String> resultList = new ArrayList<String>();
            for(IValueListHelper lister : listers)
                resultList.addAll(lister.listAllValues(factor));
            result.put("values", new JSONArray(resultList));
        } else {
            int nlimit = 100;
            try {
                nlimit = Integer.parseInt(request.getParameter("limit"));
                if(nlimit > 1000)
                    nlimit = 1000;
            } catch(Exception e) {
                // just ignore
            }
            String[] queries = request.getParameterValues("q");
            JSONObject values = new JSONObject();
            result.put("completions", values);
            
            for(String query : queries) {
                if (query == null)
                    query = "";
                if (query.startsWith("\"")) {
                    query = query.substring(1);
                }
                if (query.endsWith("\"")) {
                    query = query.substring(0, query.length() - 1);
                }

                Map<String,String> filters = new HashMap<String,String>();
                String[] filtps = request.getParameterValues("f");
                if(filtps != null)
                    for(String filter : filtps) {
                        filters.put(filter, request.getParameter(filter));
                    }

                List<AutoCompleteItem> resultList = new ArrayList<AutoCompleteItem>();
                for(IValueListHelper lister : listers)
                    if(resultList.size() < nlimit) {
                        resultList.addAll(lister.autoCompleteValues(
                                factor,
                                query,
                                nlimit - resultList.size(),
                                filters
                        ));
                    }
                values.put(query, new JSONArray(resultList, true));
            }
        }

        return result;
    }
}

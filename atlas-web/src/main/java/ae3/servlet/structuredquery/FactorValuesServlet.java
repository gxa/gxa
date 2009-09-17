package ae3.servlet.structuredquery;

import ae3.restresult.RestOut;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.service.structuredquery.AutoCompleteItem;
import ae3.service.structuredquery.IValueListHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pashky
 */
public class FactorValuesServlet extends RestServlet {

    @RestOut(xmlItemName = "completion")
    public static class ACList extends ArrayList<AutoCompleteItem> {}
    @RestOut(xmlItemName = "query", xmlAttr = "id")
    public static class ACMap extends HashMap<String,List<AutoCompleteItem>> {}

    public Object process(HttpServletRequest request) {

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

        Map<String,Object> result = new HashMap<String,Object>();

        String factor = request.getParameter("factor");
        result.put("factor", factor);

        if("all".equals(request.getParameter("mode")))
        {

            List<String> resultList = new ArrayList<String>();
            for(IValueListHelper lister : listers)
                resultList.addAll(lister.listAllValues(factor));
            result.put("values", resultList);
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

            Map<String,List<AutoCompleteItem>> values = new ACMap();
            result.put("completions", values);
            
            for(String query : queries) {
                String q = query != null ? query : "";
                if (q.startsWith("\"")) {
                    q = q.substring(1);
                }
                if (q.endsWith("\"")) {
                    q = q.substring(0, q.length() - 1);
                }

                Map<String,String> filters = new HashMap<String,String>();
                String[] filtps = request.getParameterValues("f");
                if(filtps != null)
                    for(String filter : filtps) {
                        filters.put(filter, request.getParameter(filter));
                    }

                List<AutoCompleteItem> resultList = new ACList();
                for(IValueListHelper lister : listers)
                    if(resultList.size() < nlimit) {
                        resultList.addAll(lister.autoCompleteValues(
                                factor,
                                query,
                                nlimit - resultList.size(),
                                filters
                        ));
                    }
                values.put(query != null ? query : "", resultList);
            }
        }

        return result;
    }
}

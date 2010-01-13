package ae3.servlet.structuredquery;

import ae3.restresult.RestOut;
import ae3.service.structuredquery.AutoCompleteItem;
import ae3.service.structuredquery.AutoCompleter;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

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
    public static class ACList extends ArrayList<AutoCompleteItem> {
    }

    @RestOut(xmlItemName = "query", xmlAttr = "id")
    public static class ACMap extends HashMap<String, List<AutoCompleteItem>> {
    }

    public Object process(HttpServletRequest request) {

        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());

        List<AutoCompleter> listers = new ArrayList<AutoCompleter>();

        String type = request.getParameter("type");
        if ("gene".equals(type)) {
            listers.add((AutoCompleter)context.getBean("atlasGenePropertyService"));
        }
        else if ("efv".equals(type)) {
            listers.add((AutoCompleter)context.getBean("atlasEfvService"));
        }
        else if ("efo".equals(type)) {
            listers.add((AutoCompleter)context.getBean("atlasEfoService"));
        }
        else if ("efoefv".equals(type)) {
            listers.add((AutoCompleter)context.getBean("atlasEfoService"));
            listers.add((AutoCompleter)context.getBean("atlasEfvService"));
        }

        Map<String, Object> result = new HashMap<String, Object>();

        String factor = request.getParameter("factor");
        if(factor == null)
            factor = "";
        result.put("factor", factor);

        int nlimit = 100;
        try {
            nlimit = Integer.parseInt(request.getParameter("limit"));
            if (nlimit > 1000) {
                nlimit = 1000;
            }
        }
        catch (Exception e) {
            // just ignore
        }
        String[] queries = request.getParameterValues("q");

        Map<String, List<AutoCompleteItem>> values = new ACMap();
        result.put("completions", values);

        for (String query : queries) {
            String q = query != null ? query : "";
            if (q.startsWith("\"")) {
                q = q.substring(1);
            }
            if (q.endsWith("\"")) {
                q = q.substring(0, q.length() - 1);
            }

            Map<String, String> filters = new HashMap<String, String>();
            String[] filtps = request.getParameterValues("f");
            if (filtps != null) {
                for (String filter : filtps) {
                    filters.put(filter, request.getParameter(filter));
                }
            }

            List<AutoCompleteItem> resultList = new ACList();
            for (AutoCompleter lister : listers) {
                if (resultList.size() < nlimit) {
                    resultList.addAll(lister.autoCompleteValues(
                            factor,
                            q,
                            nlimit - resultList.size(),
                            filters
                    ));
                }
            }
            values.put(q != null ? q : "", resultList);
        }

        return result;
    }
}

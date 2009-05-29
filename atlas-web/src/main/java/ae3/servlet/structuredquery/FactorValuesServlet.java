package ae3.servlet.structuredquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import ae3.service.structuredquery.IValueListHelper;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.service.structuredquery.AutoCompleteItem;

/**
 * @author pashky
 */
public class FactorValuesServlet extends HttpServlet {
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
        
        if("all".equals(request.getParameter("mode")))
        {
            for(IValueListHelper lister : listers)
                for(String fv : lister.listAllValues(request.getParameter("factor")))
                    response.getWriter().println(fv);
        } else {
            int nlimit = 100;
            try {
                nlimit = Integer.parseInt(request.getParameter("limit"));
                if(nlimit > 1000)
                    nlimit = 1000;
            } catch(Exception e) {
                // just ignore
            }
            String factor = request.getParameter("factor");
            String query = request.getParameter("q");
            if (query == null)
                query = "";
            if (query.startsWith("\"")) {
                query = query.substring(1);
            }
            if (query.endsWith("\"")) {
                query = query.substring(0, query.length() - 1);
            }
            for(IValueListHelper lister : listers) {
                Iterable<AutoCompleteItem> ac =
                        lister.autoCompleteValues(
                                factor,
                                query,
                                nlimit
                        );
                for(AutoCompleteItem s : ac) {
                    response.getWriter().println(
                            (s.getProperty() == null ? "" : s.getProperty() + "|") +
                                    s.getValue() + "|" + s.getCount() +
                                    (s.getComment() == null ? "" : "|" + s.getComment()));
                }
            }
        }
    }
}

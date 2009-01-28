package ae3.servlet.structuredquery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Writer;
import java.io.PrintWriter;
import java.util.Map;

import ae3.service.structuredquery.IValueListHelper;
import ae3.service.structuredquery.AtlasStructuredQueryService;

/**
 * @author pashky
 */
public class FactorValues extends HttpServlet {
    private Log log = LogFactory.getLog(getClass());

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doIt(httpServletRequest, httpServletResponse);
    }

    private void doIt(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("utf-8");

        AtlasStructuredQueryService service = ae3.service.ArrayExpressSearchService.instance()
                .getStructQueryService();
        
        IValueListHelper lister = "gene".equals(request.getParameter("type")) ?
                service.getGeneListHelper() : service.getEfvListHelper();
        
        if("all".equals(request.getParameter("mode")))
        {
            for(String fv : lister.listAllValues(request.getParameter("factor")))
            {
                response.getWriter().println(fv);
            }
        } else {
            int nlimit = 100;
            try {
                nlimit = Integer.parseInt(request.getParameter("limit"));
                if(nlimit > 1000)
                    nlimit = 1000;
            } catch(Exception e) {
                // just ignore
            }
            Map<String,Long> ac =
                    lister.autoCompleteValues(
                            request.getParameter("factor"),
                            request.getParameter("q"),
                            nlimit
                    );
            if (ac.size() == 0) {
                log.info("No completions found");
            }
            for(Map.Entry<String,Long> s : ac.entrySet()) {
                response.getWriter().println(s.getKey() + "|" + s.getValue());
            }
        }
    }
}

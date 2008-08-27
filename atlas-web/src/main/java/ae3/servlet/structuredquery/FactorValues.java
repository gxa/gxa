package ae3.servlet.structuredquery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

/**
 * @author pashky
 */
public class FactorValues extends HttpServlet {
    private Log log = LogFactory.getLog(getClass());

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doPost(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("utf-8");
        int nlimit = 100;
        try {
            nlimit = Integer.parseInt(request.getParameter("limit"));
            if(nlimit > 1000)
                nlimit = 1000;
        } catch(Exception e) {
            // just ignore
        }

        Map<String,Long> ac = ae3.service.ArrayExpressSearchService.instance()
                .autoCompleteFactorValues(
                        request.getParameter("factor"),
                        request.getParameter("q"),
                        nlimit
                );
        if (ac != null) {
            for(Map.Entry<String,Long> s : ac.entrySet()) {
                response.getWriter().println(s.getKey() + "|" + s.getValue());
            }
        } else {
            log.info("No completions found");
        }
    }
}

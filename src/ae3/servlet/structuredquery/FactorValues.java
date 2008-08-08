package ae3.servlet.structuredquery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

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
        if(!"".equals(request.getParameter("factor")))
        {
            java.util.List<String> ac = ae3.service.ArrayExpressSearchService.instance()
                    .autoCompleteFactorValues(
                            request.getParameter("factor"),
                            request.getParameter("q"),
                            request.getParameter("limit")
                    );
            if (ac != null) {
                for(String s : ac) {
                    response.getWriter().println(s);
                }
            } else {
                log.info("No completions found");
            }
        }
    }
}

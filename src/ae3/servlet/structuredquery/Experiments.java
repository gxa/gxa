package ae3.servlet.structuredquery;

import ae3.service.ArrayExpressSearchService;
import ae3.service.AtlasExperimentRow;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Serve experiments list for AJAX support in structured query page
 * Parameters:
 *     ef - factior
 *     efv - list of factor values
 *     gene - gene id key
 *     updn - either 1 or -1
 * Result:
 *     JSON-serialized list of matching experiments and atlas data too
 * @author pashky
 */
public class Experiments extends HttpServlet {
    private Log log = LogFactory.getLog(getClass());

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doPost(httpServletRequest, httpServletResponse);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String factor = request.getParameter("ef");
        String[] factorValues = request.getParameterValues("efv");
        String geneIdKey = request.getParameter("gene");
        String updn = request.getParameter("updn");

        List<AtlasExperimentRow> experiments = ArrayExpressSearchService.instance().
                getExperiments(geneIdKey, factor, factorValues, updn);

        JSONArray json = new JSONArray(experiments, false);
        try {
            json.write(response.getWriter());
        } catch (JSONException e) {
            log.error("Can't serialize to JSON", e);
        }
    }
}

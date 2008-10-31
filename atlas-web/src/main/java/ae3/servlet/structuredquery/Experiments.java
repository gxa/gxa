package ae3.servlet.structuredquery;

import ae3.service.ArrayExpressSearchService;
import ae3.service.AtlasExperimentRow;
import ae3.service.structuredquery.AtlasStructuredQueryParser;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

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

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException
    {
        doPost(httpServletRequest, httpServletResponse);
    }

    static public class UpDownPair {
        private List<AtlasExperimentRow> ups;
        private List<AtlasExperimentRow> downs;

        private UpDownPair(List<AtlasExperimentRow> ups, List<AtlasExperimentRow> downs) {
            this.ups = ups;
            this.downs = downs;
        }

        public List<AtlasExperimentRow> getUps() {
            return ups;
        }

        public List<AtlasExperimentRow> getDowns() {
            return downs;
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String geneIdKey = request.getParameter("gene");
        if(geneIdKey == null || "".equals(geneIdKey))
            return;

        // Don't know how to make it with real types, not Object
        List<UpDownPair> experiments = new ArrayList<UpDownPair>();
        for(String p : AtlasStructuredQueryParser.findPrefixParamsSuffixes(request, "ef")) {
            String factor = request.getParameter("ef" + p);
            String[] factorValues = request.getParameterValues("fv" + p);
            if(factor != null && factorValues != null)
                for(String factorValue : factorValues)
                    experiments.add(new UpDownPair(
                            ArrayExpressSearchService.instance().getExperiments(geneIdKey, factor, factorValue, "1"),
                            ArrayExpressSearchService.instance().getExperiments(geneIdKey, factor, factorValue, "-1")
                    ));
        }

        try {
            new JSONArray(experiments, false).write(response.getWriter());
        } catch (JSONException e) {
            log.error("Can't serialize to JSON", e);
        }
    }
}

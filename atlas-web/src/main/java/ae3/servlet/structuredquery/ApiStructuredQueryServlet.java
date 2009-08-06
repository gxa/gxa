package ae3.servlet.structuredquery;

import com.jamesmurty.utils.XMLBuilder;

import javax.servlet.http.HttpServletRequest;

import ae3.service.structuredquery.*;
import ae3.service.ArrayExpressSearchService;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * @author pashky
 */
public class ApiStructuredQueryServlet extends JsonServlet {

    public JSONObject process(HttpServletRequest request) throws JSONException {
        JSONObject jsResult = new JSONObject();
        final AtlasStructuredQueryService asqs = ArrayExpressSearchService.instance().getStructQueryService();

        AtlasStructuredQuery atlasQuery = AtlasStructuredQueryParser.parseRestRequest(request,
                GeneProperties.allPropertyIds(),
                asqs.getExperimentalFactors());

        if(!atlasQuery.isNone()) {
            AtlasStructuredQueryResult atlasResult = asqs.doStructuredAtlasQuery(atlasQuery);

            jsResult.put("totalResults", atlasResult.getTotal());
            jsResult.put("rows", atlasResult.getSize());
            jsResult.put("startingFrom", atlasResult.getStart());

            jsResult.put("efvs", atlasResult.getResultEfvs().toString());
            jsResult.put("efos", atlasResult.getResultEfos().toString());
        } else {
            jsResult.put("error", "Empty query specified");
        }

        return jsResult;
    }

}

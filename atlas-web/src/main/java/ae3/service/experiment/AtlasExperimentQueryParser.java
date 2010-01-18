package ae3.service.experiment;

import static uk.ac.ebi.gxa.utils.EscapeUtil.parseNumber;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


/**
 * API experiment search query parser class. Has just one static method
 * @author pashky
 */
public class AtlasExperimentQueryParser {
    /**
     * Parse HTTP request into AtlasExperimentQuery class
     * @param request HTTP Servlet request to parse
     * @param factors a list of all factors
     * @return AtlasExperimentQuery object, can be empty (check with isEmpty() method) but never null
     */
    public static AtlasExperimentQuery parse(HttpServletRequest request, Iterable<String> factors) {
        AtlasExperimentQuery query = new AtlasExperimentQuery();

        for(Object e  : request.getParameterMap().entrySet()) {
            String name = ((Map.Entry)e).getKey().toString();
            for(String v : ((String[])((Map.Entry)e).getValue())) {
                if(name.matches("^experiment(Text|Id|Accession)?$")) {
                    if(v.equalsIgnoreCase("listAll"))
                        query.listAll();
                    else
                        query.andText(v);
                } else if(name.matches("^experimentHasFactor$")) {
                    query.andHasFactor(v);
                } else if(name.matches("^experimentHas.*$")) {
                    String factName = name.substring("experimentHas".length()).toLowerCase();
                    if(factName.startsWith("any"))
                        factName = "";
                    else if(factName.length() > 0)
                        for(String p : factors)
                            if(p.equalsIgnoreCase(factName))
                                factName = p;

                    query.andHasFactorValue(factName, v);
                } else if(name.equalsIgnoreCase("rows")) {
                    query.rows(parseNumber(v, 10, 1, 200));
                } else if(name.equalsIgnoreCase("start")) {
                    query.start(parseNumber(v, 0, 0, Integer.MAX_VALUE));
                }
            }
        }

        return query;
    }
}

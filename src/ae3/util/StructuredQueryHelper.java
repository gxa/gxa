package ae3.util;

import ae3.service.AtlasStructuredQuery;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Helper functions for parsing and managing structured query
 * @author pashky
 */
public class StructuredQueryHelper {
    private static String PARAM_REGULATION = "gexp_";
    private static String PARAM_FACTOR = "fact_";
    private static String PARAM_FACTORVALUE = "fval_";
    private static String PARAM_GENE = "gene";

    @SuppressWarnings("unchecked")
    private static List<String> parseSpecies(final HttpServletRequest httpRequest)
    {
        List<String> result = new ArrayList<String>();

        for(Map.Entry<String,String[]> p : ((Map<String,String[]>)httpRequest.getParameterMap()).entrySet())
            if(p.getKey().startsWith("specie_"))
            {
                if(p.getValue()[0].length() == 0)
                    // "any" value found, return magic empty list
                    return new ArrayList<String>();
                else
                    result.add(p.getValue()[0]);
            }
        return result;
    }

    private static List<AtlasStructuredQuery.Condition> parseConditions(final HttpServletRequest httpRequest)
    {
        List<AtlasStructuredQuery.Condition> result = new ArrayList<AtlasStructuredQuery.Condition>();

        @SuppressWarnings("unchecked")
        Map<String,String[]> params = httpRequest.getParameterMap();
        for(Map.Entry<String,String[]> p : params.entrySet())
            if(p.getKey().startsWith(PARAM_REGULATION))
            {
                AtlasStructuredQuery.Condition condition = new AtlasStructuredQuery.Condition();
                String id = p.getKey().substring(PARAM_REGULATION.length());
                try {
                    condition.setRegulation(AtlasStructuredQuery.Regulation.valueOf(p.getValue()[0]));

                    String factor = httpRequest.getParameter(PARAM_FACTOR + id);
                    if(factor == null || factor.length() == 0)
                        throw new IllegalArgumentException("Empty factor name rowid:" + id);

                    condition.setFactor(factor);

                    List<String> values = new ArrayList<String>();
                    for(Map.Entry<String,String[]> v : params.entrySet())
                        if(v.getKey().startsWith(PARAM_FACTORVALUE + id + "_") && v.getValue()[0].length() > 0)
                            values.add(v.getValue()[0]);

                    if(values.size() == 0)
                        throw new IllegalArgumentException("No values specified for factor " + factor + " rowid:" + id);

                    condition.setFactorValues(values);
                    result.add(condition);
                } catch (IllegalArgumentException e) {
                    // Ignore this one, may be better stop future handling
                    e.printStackTrace();
                }
            }

        return result;
    }

   /**
    * Parse HTTP request parameters and build AtlasExtendedRequest structure
    * @param httpRequest HTTP servlet request
    * @return extended request made of succesfully parsed conditions
    */
    static public AtlasStructuredQuery parseRequest(final HttpServletRequest httpRequest)
    {
        AtlasStructuredQuery request = new AtlasStructuredQuery();
        String gene = httpRequest.getParameter(PARAM_GENE);
        if(gene == null)
            return null;
        request.setGene(gene.equals("(all genes)") ? "" : gene);
        request.setSpecies(parseSpecies(httpRequest));
        request.setConditions(parseConditions(httpRequest));
        return request;
    }
}

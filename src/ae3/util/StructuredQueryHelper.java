package ae3.util;

import ae3.service.AtlasStructuredQuery;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Helper functions for parsing and managing structured query
 * @author pashky
 */
public class StructuredQueryHelper {
    private static String PARAM_EXPRESSION = "gexp_";
    private static String PARAM_FACTOR = "fact_";
    private static String PARAM_FACTORVALUE = "fval_";
    private static String PARAM_GENE = "gene";
    private static String PARAM_SPECIE = "specie_";

    private static List<String> findPrefixParamsSuffixes(final HttpServletRequest httpRequest, final String prefix)
    {
        List<String> result = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        Enumeration<String> e = httpRequest.getParameterNames();
        while(e.hasMoreElements()) {
            String v = e.nextElement();
            if(v.startsWith(prefix))
                result.add(v.replace(prefix, ""));
        }
        Collections.sort(result, new Comparator<String>() {
            public int compare(String o1, String o2) {
                try {
                    return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
                } catch(NumberFormatException e) {
                    return o1.compareTo(o2);
                }
            }
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseSpecies(final HttpServletRequest httpRequest)
    {
        List<String> result = new ArrayList<String>();

        for(String p : findPrefixParamsSuffixes(httpRequest, PARAM_SPECIE)) {
            String value = httpRequest.getParameter(PARAM_SPECIE + p);
            if(value.length() == 0)
                // "any" value found, return magic empty list
                return new ArrayList<String>();
            else
                result.add(value);
        }
        return result;
    }

    private static List<AtlasStructuredQuery.Condition> parseConditions(final HttpServletRequest httpRequest)
    {
        List<AtlasStructuredQuery.Condition> result = new ArrayList<AtlasStructuredQuery.Condition>();

        for(String id : findPrefixParamsSuffixes(httpRequest, PARAM_FACTOR)) {
            AtlasStructuredQuery.Condition condition = new AtlasStructuredQuery.Condition();
            try {
                condition.setExpression(AtlasStructuredQuery.Expression.valueOf(httpRequest.getParameter(PARAM_EXPRESSION + id)));

                String factor = httpRequest.getParameter(PARAM_FACTOR + id);
                if(factor == null || factor.length() == 0)
                    throw new IllegalArgumentException("Empty factor name rowid:" + id);

                condition.setFactor(factor);

                List<String> values = new ArrayList<String>();
                String pfx = PARAM_FACTORVALUE + id + "_";
                for(String jd : findPrefixParamsSuffixes(httpRequest, pfx)) {
                    String value = httpRequest.getParameter(pfx + jd);
                    if(value.length() > 0)
                        values.add(value);
                }

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

package ae3.util;

import ae3.service.AtlasStructuredQuery;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper functions for parsing and managing structured query
 * @author pashky
 */
public class StructuredQueryHelper {
    static protected final Log log = LogFactory.getLog("StructuredQueryHelper");

    private static String PARAM_EXPRESSION = "gexp_";
    private static String PARAM_FACTOR = "fact_";
    private static String PARAM_FACTORVALUE = "fval_";
    private static String PARAM_GENE = "gene";
    private static String PARAM_SPECIE = "specie_";

    public static List<String> findPrefixParamsSuffixes(final HttpServletRequest httpRequest, final String prefix)
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
                if(factor == null)
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
                log.error("Unable to parse and condition. Ignoring it.", e);
            }
        }

        return result;
    }

   /**
    * Parse HTTP request parameters and build AtlasExtendedRequest structure
    * @param httpRequest HTTP servlet request
    * @return extended request made of succesfully parsed conditions
    */
    static public AtlasStructuredQuery parseRequest(final HttpServletRequest httpRequest) {
        AtlasStructuredQuery request = new AtlasStructuredQuery();
        String gene = httpRequest.getParameter(PARAM_GENE);
        if(gene == null)
            return null;
        request.setGene(gene.equals("(all genes)") ? "" : gene);
        request.setSpecies(parseSpecies(httpRequest));
        request.setConditions(parseConditions(httpRequest));
        return request;
    }

    /**
     * Encode staring with URL encdoing (%xx's)
     * @param str url
     * @return encoded str
     */
    public static String escapeURL(String str)
    {
        try
        {
            return URLEncoder.encode(str, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    /**
     * Returns array of gradually spread colors from rgb1 to rgb2,
     * the gradient is proportionally split according to up:down ratio
     * @param r1 red component of color 1
     * @param g1 green component of color 1
     * @param b1 blue component of color 1
     * @param r2 red component of color 2
     * @param g2 green component of color 2
     * @param b2 blue component of color 2
     * @param n number of steps
     * @param up number of ups
     * @param down number of downs
     * @return array of hex html colors
     */
    public static String[] gradient(int r1, int g1, int b1, int r2, int g2, int b2, int n, int up, int down)
    {
        String[] result = new String[n];

        double x = 0;
        if(up + down > 0)
            x = (double)up / (double)(up+down);

        int k = 3 + (int)(x * (double)(n - 7));
        for(int i = 0; i < n; ++i)
        {
            int r, g, b;
            if(i < k)
            {
                r = r1 + (r2-r1) * i / 2 / k;
                g = g1 + (g2-g1) * i / 2 / k;
                b = b1 + (b2-b1) * i / 2 / k;
            } else {
                r = (r1 + r2 + (r2-r1) * (i - k) / (n - 1 - k)) / 2;
                g = (g1 + g2 + (g2-g1) * (i - k) / (n - 1 - k)) / 2;
                b = (b1 + b2 + (b2-b1) * (i - k) / (n - 1 - k)) / 2;
            }
            result[i] = String.format("#%02x%02x%02x", r, g, b);
        }
        return result;
    }

    /**
     * Returns current system time, for util.tld
     * @return time in milliseconds
     */
    public static long currentTime()
    {
        return System.currentTimeMillis();
    }
}

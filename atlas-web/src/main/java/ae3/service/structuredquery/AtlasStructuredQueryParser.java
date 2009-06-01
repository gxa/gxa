package ae3.service.structuredquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import ae3.util.AtlasProperties;
import ae3.util.EscapeUtil;

/**
 * @author pashky
 */
public class AtlasStructuredQueryParser {
    private static final Logger log = LoggerFactory.getLogger(AtlasStructuredQueryParser.class);
    private static final String PARAM_EXPRESSION = "fexp_";
    private static final String PARAM_FACTOR = "fact_";
    private static final String PARAM_FACTORVALUE = "fval_";
    private static final String PARAM_GENE = "gval_";
    private static final String PARAM_GENENOT = "gnot_";
    private static final String PARAM_GENEPROP = "gprop_";
    private static final String PARAM_SPECIE = "specie_";
    private static final String PARAM_START = "p";
    private static final String PARAM_EXPAND = "fexp";

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

    private static List<ExpFactorQueryCondition> parseExpFactorConditions(final HttpServletRequest httpRequest)
    {
        List<ExpFactorQueryCondition> result = new ArrayList<ExpFactorQueryCondition>();

        for(String id : findPrefixParamsSuffixes(httpRequest, PARAM_FACTOR)) {
            ExpFactorQueryCondition condition = new ExpFactorQueryCondition();
            try {
                condition.setExpression(QueryExpression.valueOf(httpRequest.getParameter(PARAM_EXPRESSION + id)));

                String factor = httpRequest.getParameter(PARAM_FACTOR + id);
                if(factor == null)
                    throw new IllegalArgumentException("Empty factor name rowid:" + id);

                condition.setFactor(factor);

                String value = httpRequest.getParameter(PARAM_FACTORVALUE + id);
                List<String> values = value != null ? EscapeUtil.parseQuotedList(value) : new ArrayList<String>();

                condition.setFactorValues(values);
                result.add(condition);
            } catch (IllegalArgumentException e) {
                // Ignore this one, may be better stop future handling
                log.error("Unable to parse and condition. Ignoring it.", e);
            }
        }
        return result;
    }

    private static List<GeneQueryCondition> parseGeneConditions(final HttpServletRequest httpRequest)
    {
        List<GeneQueryCondition> result = new ArrayList<GeneQueryCondition>();

        for(String id : findPrefixParamsSuffixes(httpRequest, PARAM_GENEPROP)) {
            GeneQueryCondition condition = new GeneQueryCondition();
            try {
                String not = httpRequest.getParameter(PARAM_GENENOT + id);
                condition.setNegated(not != null && !"".equals(not) && !"0".equals(not));

                String factor = httpRequest.getParameter(PARAM_GENEPROP + id);
                if(factor == null)
                    throw new IllegalArgumentException("Empty gene property name rowid:" + id);

                condition.setFactor(factor);

                String value = httpRequest.getParameter(PARAM_GENE + id);
                List<String> values = value != null ? EscapeUtil.parseQuotedList(value) : new ArrayList<String>();
                if(values.size() > 0)
                {
                    condition.setFactorValues(values);
                    result.add(condition);
                }
            } catch (IllegalArgumentException e) {
                // Ignore this one, may be better stop future handling
                log.error("Unable to parse and condition. Ignoring it.", e);
            }
        }

        return result;
    }

    static private Set<String> parseExpandColumns(final HttpServletRequest httpRequest)
    {
        String[] values = httpRequest.getParameterValues(PARAM_EXPAND);
        Set<String> result = new HashSet<String>();
        if(values != null && values.length > 0)
        {
            result.addAll(Arrays.asList(values));
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
        request.setGeneConditions(parseGeneConditions(httpRequest));

        request.setSpecies(parseSpecies(httpRequest));
        request.setConditions(parseExpFactorConditions(httpRequest));
        request.setView(httpRequest.getParameter("view"));
        
        if(httpRequest.getParameter("export")!=null){
        	request.setExport(httpRequest.getParameter("export").equals("true"));
        }
        
        if(!request.isNone()){
        	if(request.viewHeatMap() || request.isExport())
            	request.setRowsPerPage(AtlasProperties.getIntProperty("atlas.query.pagesize"));
            else 
            	request.setRowsPerPage(AtlasProperties.getIntProperty("atlas.query.listsize")); //TO DO: put value in atlas properties
        }
        
        
        String start = httpRequest.getParameter(PARAM_START);
        try {
            request.setStart(Integer.valueOf(start) * request.getRowsPerPage());
        } catch(Exception e) {
            request.setStart(0);
        }

        request.setExpandColumns(parseExpandColumns(httpRequest));
       

        return request;
    }

}

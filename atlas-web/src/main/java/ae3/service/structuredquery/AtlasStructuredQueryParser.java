package ae3.service.structuredquery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;

/**
 * @author pashky
 */
public class AtlasStructuredQueryParser {
    private static final Log log = LogFactory.getLog("AtlasStructuredQueryParser");
    private static String PARAM_EXPRESSION = "gexp_";
    private static String PARAM_EXPRESSION_SIMPLE = "gexp";
    private static String PARAM_FACTOR = "fact_";
    private static String PARAM_FACTORVALUE = "fval_";
    private static String PARAM_FACTORVALUE_SIMPLE = "fval";
    private static String PARAM_GENE = "gene_";
    private static String PARAM_GENEPROP = "geneprop_";
    private static String PARAM_GENEOPERTOR = "geneoperator_";
    private static String PARAM_SPECIE = "specie_";
    private static String PARAM_SPECIE_SIMPLE = "specie";
    private static int DEFAULT_ROWS = 100;
    private static String PARAM_START = "p";
    private static String PARAM_EXPAND = "fexp";

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

        String specieSimple = httpRequest.getParameter(PARAM_SPECIE_SIMPLE);
        if("".equals(specieSimple))
            return new ArrayList<String>();
        if(specieSimple != null)
            result.add(specieSimple);
        
        return result;
    }

    private static List<AtlasStructuredQuery.Condition> parseConditions(final HttpServletRequest httpRequest)
    {
        if(httpRequest.getParameter(PARAM_FACTORVALUE_SIMPLE) != null)
            return parseConditionsSimple(httpRequest);
        else
            return parseConditionsStruct(httpRequest);
    }
    
    private static List<AtlasStructuredQuery.Condition> parseConditionsStruct(final HttpServletRequest httpRequest)
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
                    if(value != null)
                        values.add(StringUtils.trim(value));
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

    private static List<AtlasStructuredQuery.Condition> parseConditionsSimple(final HttpServletRequest httpRequest)
    {
        List<AtlasStructuredQuery.Condition> result = new ArrayList<AtlasStructuredQuery.Condition>(1);
        AtlasStructuredQuery.Condition cond = new AtlasStructuredQuery.Condition();

        try {
            String fval = httpRequest.getParameter(PARAM_FACTORVALUE_SIMPLE);
            if("(all conditions)".equals(fval))
                return result;
            
            Reader r = new StringReader(fval);
            List<String> values = new ArrayList<String>();
            StringBuffer curVal = new StringBuffer();
            boolean inQuotes = false;
            while(true) {
                int c = r.read();
                if(inQuotes)
                {
                    if(c < 0)
                        return result; // skip last incorrect condition

                    if(c == '\\') {
                        c = r.read();
                        if(c < 0)
                            return result; // skip last incorrect condition
                            
                        curVal.appendCodePoint(c);
                    } else if(c == '"') {
                        inQuotes = false;
                    } else {
                        curVal.appendCodePoint(c);
                    }
                } else {
                    if(c < 0  || Character.isSpaceChar(c))
                    {
                        if(curVal.length() > 0) {
                            values.add(curVal.toString());
                            curVal.setLength(0);
                        }
                    } else if(c == '"') {
                        inQuotes = true;
                    } else {
                        curVal.appendCodePoint(c);
                    }
                    
                    if(c < 0)
                        break;
                }
            }
            cond.setFactorValues(values);

            AtlasStructuredQuery.Expression expression = AtlasStructuredQuery.Expression.UP_DOWN;
            try {
                expression = AtlasStructuredQuery.Expression.valueOf(httpRequest.getParameter(PARAM_EXPRESSION_SIMPLE));
            } catch(Exception e) {
                // ignore
            }
            cond.setExpression(expression);
            cond.setFactor("");

            result.add(cond);
        } catch (IOException e) {
            throw new RuntimeException("Shouldn't be", e);
        }

        return result;
    }
    
   
    private static List<AtlasStructuredQuery.GeneQuery> parseGeneQuery(final HttpServletRequest httpRequest){
    	List<AtlasStructuredQuery.GeneQuery> result = new ArrayList<AtlasStructuredQuery.GeneQuery>();
    	int geneCount = findPrefixParamsSuffixes(httpRequest,PARAM_GENE).size();
    	for(int id=0; id<geneCount; id++){
    		AtlasStructuredQuery.GeneQuery geneQuery = new AtlasStructuredQuery.GeneQuery();
    		try {
    			String qry = httpRequest.getParameter(PARAM_GENE + id);
    			if(qry==null){
    				throw new IllegalArgumentException("Empty gene query:" + id);
    			}
    			if(qry.contains(" ") && !qry.startsWith("\\\""))
    				qry="\""+qry+"\"";
    			geneQuery.setQry(qry);
    			String property = httpRequest.getParameter(PARAM_GENEPROP + id);
   				geneQuery.setProperty(property);
    			
    			String operator = httpRequest.getParameter(PARAM_GENEOPERTOR + id);
    			if(operator==null){
    				if(id!=0 && id!=geneCount)
    					throw new IllegalArgumentException("Empty logical operator:" + id);
    				else
    					operator="";
    			}
    			else
    				geneQuery.setOperator(operator);
    			result.add(geneQuery);
    		}catch(IllegalArgumentException e){
    			log.error("Unable to parse gene query.",e);
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
        
        List<AtlasStructuredQuery.GeneQuery> geneQueries = parseGeneQuery(httpRequest);
        if(geneQueries.isEmpty())
        	return null;
        
        request.setGeneQueries(geneQueries);

        request.setSpecies(parseSpecies(httpRequest));
        request.setConditions(parseConditions(httpRequest));
        request.setRows(DEFAULT_ROWS);

        String start = httpRequest.getParameter(PARAM_START);
        try {
            request.setStart(Integer.valueOf(start));
        } catch(Exception e) {
            request.setStart(0);
        }

        request.setExpandColumns(parseExpandColumns(httpRequest));

        return request;
    }

}

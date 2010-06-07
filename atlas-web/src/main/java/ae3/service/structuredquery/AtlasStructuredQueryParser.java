/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package ae3.service.structuredquery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import static uk.ac.ebi.gxa.utils.EscapeUtil.parseNumber;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @author pashky
 */
public class AtlasStructuredQueryParser {
    private static final Logger log = LoggerFactory.getLogger(AtlasStructuredQueryParser.class);
    private static final String PARAM_EXPRESSION = "fexp_";
    private static final String PARAM_MINEXPERIMENTS = "fmex_";
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
                condition.setMinExperiments(EscapeUtil.parseNumber(httpRequest.getParameter(PARAM_MINEXPERIMENTS + id), 1, 1, Integer.MAX_VALUE));

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

    static private ViewType parseViewType(String s) {
        try {
            if("list".equals(s))
                return ViewType.LIST;
        } catch (Exception e) { // skip
        }
        return ViewType.HEATMAP;
    }

    /**
     * Parse HTTP request parameters and build AtlasExtendedRequest structure
     * @param httpRequest HTTP servlet request
     * @return extended request made of succesfully parsed conditions
     */
    static public AtlasStructuredQuery parseRequest(final HttpServletRequest httpRequest,
                                                    final AtlasProperties atlasProperties) {
        AtlasStructuredQuery request = new AtlasStructuredQuery();
        request.setGeneConditions(parseGeneConditions(httpRequest));

        request.setSpecies(parseSpecies(httpRequest));
        request.setConditions(parseExpFactorConditions(httpRequest));
        request.setViewType(parseViewType(httpRequest.getParameter("view")));


        if(!request.isNone()){
        	if(request.getViewType() == ViewType.HEATMAP)
            	request.setRowsPerPage(atlasProperties.getQueryPageSize());
            else{
            	request.setRowsPerPage(atlasProperties.getQueryListSize());
            	request.setExpsPerGene(atlasProperties.getQueryExperimentsPerGene());
            }
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

    static public AtlasStructuredQuery parseRestRequest(HttpServletRequest request, Collection<String> properties, Collection<String> factors) {
        AtlasStructuredQueryBuilder qb = new AtlasStructuredQueryBuilder();
        qb.viewAs(ViewType.LIST);
        Pattern pexpr = Pattern.compile("^(up|d(ow)?n|up([Oo]r)?[Dd](ow)?n|up([Oo]nly)|d(ow)?n([Oo]nly)?)([0-9]*)In(.*)$");
        for(Object e  : request.getParameterMap().entrySet()) {
            String name = ((Map.Entry)e).getKey().toString();
            for(String v : ((String[])((Map.Entry)e).getValue())) {
                Matcher m;
                if(name.matches("^gene.*Is(Not)?$")) {
                    boolean not = name.endsWith("Not");
                    String propName = name.substring(4, name.length() - (not ? 5 : 2)).toLowerCase();
                    if(propName.startsWith("any"))
                        propName = "";
                    else if(propName.length() > 0)
                        for(String p : properties)
                            if(p.equalsIgnoreCase(propName))
                                propName = p;

                    qb.andGene(propName, !not, EscapeUtil.parseQuotedList(v));
                } else if((m = pexpr.matcher(name)).matches()) {
                    QueryExpression qexp = QueryExpression.parseFuzzyString(m.group(1));
                    int minExp = EscapeUtil.parseNumber(m.group(8), 1, 1, Integer.MAX_VALUE);
                    String factName = m.group(9).toLowerCase();
                    if(factName.startsWith("any"))
                        factName = "";
                    else if(factName.length() > 0)
                        for(String p : factors)
                            if(p.equalsIgnoreCase(factName))
                                factName = p;

                    qb.andExprIn(factName, qexp, minExp, EscapeUtil.parseQuotedList(v));
                } else if(name.equalsIgnoreCase("species")) {
                    qb.andSpecies(v);
                } else if(name.equalsIgnoreCase("rows")) {
                    qb.rowsPerPage(parseNumber(v, 10, 1, 200));
                } else if(name.equalsIgnoreCase("start")) {
                    qb.startFrom(parseNumber(v, 0, 0, Integer.MAX_VALUE));
                } else if(name.equalsIgnoreCase("viewAs")) {
                    try {
                        qb.viewAs(ViewType.valueOf(v.toUpperCase()));
                    } catch(Exception ee) {
                        // do nothing
                    }
                }
            }
        }
        qb.expsPerGene(Integer.MAX_VALUE);
        return qb.query();
    }

}

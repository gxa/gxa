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

package ae3.service.experiment;


import uk.ac.ebi.gxa.utils.StringUtil;

import java.util.*;

import static uk.ac.ebi.gxa.utils.EscapeUtil.escapeSolrValueList;
import static uk.ac.ebi.gxa.utils.EscapeUtil.optionalParseList;

/**
 * Atlas Experiment API query container class. Can be populated StringBuilder-style and converted to SOLR query string
 * @author pashky
 */
public class AtlasExperimentQuery {

    private final StringBuilder sb = new StringBuilder();
    private int rows = 10;
    private int start = 0;
    private boolean all = false;

    private Set<String> queryFactors = new HashSet<String>();
    private Map<String,Set<String>> queryFactorValues = new HashMap<String,Set<String>>();

    public AtlasExperimentQuery() {
        queryFactorValues.put("all", new HashSet<String>());
    }

    /**
     * Append AND to query if needed
     */
    public void and() {
        if(!isEmpty())
            sb.append(" AND ");
    }

    /**
     * Make "get all experiments" query
     * @return self
     */
    public AtlasExperimentQuery listAll() {
        sb.replace(0, sb.length(), "*:*");
        all = true;
	    rows = java.lang.Integer.MAX_VALUE;
        return this;
    }

    /**
     * Append search text to query (will be searched in accession, id and alltext fields)
     * @param text text to find
     * @return self
     */
    public AtlasExperimentQuery andText(Object text) {
        if(all)
            return this;
        and();
        List<String> texts = optionalParseList(text);
        if(!texts.isEmpty()) {
            String vals = escapeSolrValueList(texts);
            sb.append("(accession:(").append(vals)
                    .append(") id:(").append(vals)
                    .append(") ").append(vals)
                    .append(")");
        }
        return this;
    }

    /**
     * Append "has factor" condition to query
     * @param factor which factor to look for
     * @return self
     */
    public AtlasExperimentQuery andHasFactor(Object factor) {
        if(all)
            return this;
        and();
        List<String> factors = optionalParseList(factor);
        if(!factors.isEmpty())
            sb.append("a_properties:(").append(escapeSolrValueList(factors)).append(")");

        queryFactors.addAll(factors);

        for (String qfactor : factors) {
            if(!queryFactorValues.containsKey(qfactor))
                queryFactorValues.put(qfactor, new HashSet<String>());
        }

        return this;
    }

    /**
     * Append "has factor value" condition
     * @param factor factor
     * @param value value
     * @return self
     */
    public AtlasExperimentQuery andHasFactorValue(String factor, Object value) {
        if(all)
            return this;
        and();
        List<String> values = optionalParseList(value);
        if (values.isEmpty()) {
            return this;
        }
        if(StringUtil.isEmpty(factor)) {
            sb.append("a_allvalues:(").append(escapeSolrValueList(values)).append(")");
            queryFactorValues.get("all").addAll(values);
        } else {
            sb.append("a_property_").append(factor).append(":(").append(escapeSolrValueList(values)).append(")");
            queryFactorValues.get(factor).addAll(values);
        }
        return this;
    }

    /**
     * Assembles SOLR query string
     * @return
     */
    public String toSolrQuery() {
        return sb.toString();
    }

    @Override
    public String toString() {
        return toSolrQuery();
    }

    /**
     * Checks if query is empty
     * @return true or false
     */
    public boolean isEmpty() {
        return sb.length() == 0;
    }

    /**
     * Returns number of rows to fetch
     * @return number
     */
    public int getRows() {
        return rows;
    }

    /**
     * Restrict number of rows to fetch
     * @param rows number
     * @return self
     */
    public AtlasExperimentQuery rows(int rows) {
        this.rows = rows;
        return this;
    }

    /**
     * Returns start position
     * @return start position
     */
    public int getStart() {
        return start;
    }

    /**
     * Sets starting position
     * @param start start
     * @return self
     */
    public AtlasExperimentQuery start(int start) {
        this.start = start;
        return this;
    }

    /**
     * Returns factor values to search for (organised by factor)
     * @return factor values to search for (organised by factor)
     */
    public Map<String, Set<String>> getQueryFactorValues() {
        return queryFactorValues;
    }

    /**
     * Returns factors to search for
     * @return factors to search for
     */
    public Set<String> getQueryFactors() {
        return queryFactors;
    }
}

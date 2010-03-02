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


import static uk.ac.ebi.gxa.utils.EscapeUtil.optionalParseList;
import static uk.ac.ebi.gxa.utils.EscapeUtil.escapeSolrValueList;

import java.util.List;

/**
 * Atlas Experiment API query container class. Can be pupulated StringBuilder-style and converted to SOLR query string
 * @author pashky
 */
public class AtlasExperimentQuery {

    private final StringBuilder sb = new StringBuilder();
    private int rows = 10;
    private int start = 0;
    private boolean all = false;

    public void and() {
        if(!isEmpty())
            sb.append(" AND ");
    }

    public AtlasExperimentQuery listAll() {
        sb.replace(0, sb.length(), "*:*");
        all = true;
        return this;
    }

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

    public AtlasExperimentQuery andHasFactor(Object factor) {
        if(all)
            return this;
        and();
        List<String> factors = optionalParseList(factor);
        if(!factors.isEmpty())
            sb.append("a_properties:(").append(escapeSolrValueList(factors)).append(")");
        return this;
    }

    public AtlasExperimentQuery andHasFactorValue(String factor, Object value) {
        if(all)
            return this;
        and();
        List<String> values = optionalParseList(value);
        if(!values.isEmpty()) {
            if(factor == null || "".equals(factor))
                sb.append("a_allvalues:(").append(escapeSolrValueList(values)).append(")");
            else
                sb.append("a_property_").append(factor).append(":(").append(escapeSolrValueList(values)).append(")");
        }
        return this;
    }

    public String toSolrQuery() {
        return sb.toString();
    }

    @Override
    public String toString() {
        return toSolrQuery();
    }

    public boolean isEmpty() {
        return sb.length() == 0;
    }

    public int getRows() {
        return rows;
    }

    public AtlasExperimentQuery rows(int rows) {
        this.rows = rows;
        return this;
    }

    public int getStart() {
        return start;
    }

    public AtlasExperimentQuery start(int start) {
        this.start = start;
        return this;
    }
}
